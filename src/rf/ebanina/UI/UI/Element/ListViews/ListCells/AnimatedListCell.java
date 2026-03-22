package rf.ebanina.UI.UI.Element.ListViews.ListCells;

import javafx.animation.*;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Element.ListViews.ListView;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static rf.ebanina.UI.Root.general_interpolator;

//FIXME: Пофиксить баги
//FIXME: Редко, но фон не прогружается (дефолтный цвет вместо него)
public abstract class AnimatedListCell<T>
        extends ListCell<T>
{
    protected final int corners = (int) (Root.corners * 0.5);

    public Pane pane;
    protected Rectangle background;
    protected Rectangle cover;
    protected DropShadow shadow;

    protected static int maxCacheSize = 50;
    protected static final java.util.Map<String, ImagePattern> patternsCache =
            new java.util.LinkedHashMap<>(maxCacheSize, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, ImagePattern> eldest) {
                    return size() > maxCacheSize;
                }
            };

    protected static final java.util.Map<String, ImagePattern> patternsMipmapCache =
            new java.util.LinkedHashMap<>(maxCacheSize, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, ImagePattern> eldest) {
                    return size() > maxCacheSize;
                }
            };

    protected static final Map<String, Color> colorsCache = Collections.synchronizedMap(new LinkedHashMap<>(100, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > maxCacheSize;
        }
    });

    protected java.util.concurrent.Future<?> currentTask = null;
    protected java.util.concurrent.Future<?> currentBgTask = null;

    protected Timeline currentTimeline;
    protected FadeTransition showFadeAnimation;
    protected PauseTransition delayTimer;
    protected Timeline expansionTimeline;

    protected StackPane extraInfoPane;
    private double baseHeight = 24;
    private boolean isPinned = false;

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        checkOnSelected();

        showFadeAnimation().play();

        if(background != null) {
            background.setWidth(0);
        }

        if (currentTask != null)
            currentTask.cancel(true);
//        if (currentBgTask != null) // Возможно из за этого иногда фон не прогружается
//            currentBgTask.cancel(true);

        if (currentTimeline != null) {
            currentTimeline.stop();
        }

        if (delayTimer != null)
            delayTimer.stop();

        if (empty || item == null) {
            setGraphic(null);

            return;
        }

        if (extraInfoPane != null) {
            extraInfoPane.setOpacity(0);
            extraInfoPane.getChildren().clear();

            setPrefHeight(baseHeight);
            isPinned = false;
        }
    }

    public AnimatedListCell() {
        this(Color.BLACK);
    }

    public AnimatedListCell(Color mainClr) {
        bindDragAndDrop();

        setAlignment(javafx.geometry.Pos.CENTER);
        setPadding(new Insets(0));

        this.hoverProperty().addListener((obs, wasHover, isNowHover) -> {
            if (getItem() != null) {
                playAnimation(isNowHover);
            }
        });
    }

    protected void setBackgroundImageCentered(Image image, double width, Rectangle background) {
        if(image != null) {
            background.setFill(new ImagePattern(image, 0, image.getHeight() * 0.7, Math.min(width, image.getWidth()), image.getHeight(), false));
        }
    }

    protected void setBackgroundImageCentered(ImagePattern image, Rectangle background) {
        if(image != null) {
            background.setFill(image);
        }
    }

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

    public void setSelectedBackground(Border border) {
        setBorder(border);
    }

    public static Border borderFactory(Color color) {
        return new Border(new BorderStroke(
                color, BorderStrokeStyle.SOLID,
                new CornerRadii(5), new BorderWidths(2), new Insets(-2))
        );
    }

    protected void checkOnSelected() {
        if (isSelected()) {
            setSelectedBackground(borderFactory(((ListView<?>) getListView()).getSelectedColor()));

            pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), true);
        } else {
            setSelectedBackground(null);

            pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), false);
        }
    }

    protected Pane createBackgroundPane(int prefHeight) {
        this.baseHeight = prefHeight;
        this.setPrefHeight(baseHeight);
        this.setMinHeight(prefHeight);

        // -----------------------------

        shadow = new DropShadow();
        shadow.setBlurType(BlurType.GAUSSIAN);
        shadow.setOffsetX(50);
        shadow.setRadius(7);
        shadow.setWidth(255);
        shadow.setColor(Color.BLACK);

        VBox mainLayout = new VBox();
        mainLayout.setAlignment(Pos.TOP_LEFT);

        pane = new Pane();
        pane.setPrefHeight(baseHeight);

        background = new Rectangle();
        background.setLayoutX(0);
        background.setLayoutY(0);
        background.setWidth(0);
        background.setFill(ColorProcessor.core.getMainClr());
        background.heightProperty().bind(pane.heightProperty());

        extraInfoPane = new StackPane();

        mainLayout.getChildren().addAll(pane, extraInfoPane);

        // FIXME: Доработать от лишних срабатываний
        initHoverDetails();

        pane.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (background.getWidth() > 0 && !isSelected()) {
                background.setWidth(newVal.doubleValue());
            }
        });

        extraInfoPane = new StackPane();
        extraInfoPane.setOpacity(0);
        extraInfoPane.setLayoutY(baseHeight);
        extraInfoPane.layoutYProperty().bind(pane.prefHeightProperty().subtract(40));
        extraInfoPane.prefWidthProperty().bind(pane.widthProperty());

        pane.getChildren().addAll(background, extraInfoPane);

        setGraphic(pane);

        return pane;
    }

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

    protected void initCoverIcon() {
        cover = new Rectangle();
        cover.setArcWidth(corners);
        cover.setArcHeight(corners);
        cover.setHeight(pane.getPrefHeight() * 0.8);
        cover.setWidth(cover.getHeight());
        cover.setLayoutX(2);
        cover.setLayoutY(pane.getPrefHeight() / 8);
    }

    private void playAnimation(boolean isShow) {
        if (currentTimeline != null)
            currentTimeline.stop();

        Interpolator interpolator = Interpolator.SPLINE(0.15, 1, 0.25, 1);

        double fullWidth = getWidth();

        if(isShow) {
            currentTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(background.widthProperty(), background.getWidth(), interpolator)
                    ),
                    new KeyFrame(Duration.millis(800),
                            new KeyValue(background.widthProperty(), fullWidth, interpolator)
                    )
            );

            currentTimeline.play();
        } else {
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
        }
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

    protected void initHoverDetails() {
        delayTimer = new PauseTransition(Duration.millis(1500));

        delayTimer.setOnFinished(e -> {
            if (!isEmpty() && !isPinned && isHover()) {
                Node content = createExtraInfoContent();

                if (content != null) {
                    extraInfoPane.getChildren().setAll(content);
                    animateCell(baseHeight + 40, 1.0);
                }
            }
        });

        this.addEventHandler(MouseEvent.DRAG_DETECTED, e -> {
            delayTimer.stop();
            isPinned = false;
            playSquish(1.0);
            animateCell(baseHeight, 0);
        });

        this.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            if (!isEmpty())
                delayTimer.playFromStart();
        });

        this.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            delayTimer.stop();

            if (isPinned) {
                isPinned = false;
            }

            animateCell(baseHeight, 0);
        });

        this.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (extraInfoPane.getOpacity() > 0.1) {
                isPinned = true;
            }

            playSquish(0.98);
        });

        this.addEventHandler(DragEvent.DRAG_DONE, e -> {
            playSquish(1.0);
            animateCell(baseHeight, 0);
        });

        this.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            playSquish(1.0);
            if (!isHover()) {
                animateCell(baseHeight, 0);
            }
        });

        this.addEventHandler(DragEvent.DRAG_ENTERED, e -> {
            delayTimer.stop();
            isPinned = false;
            animateCell(baseHeight, 0);
        });
    }

    protected void playSquish(double scale) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), pane);
        st.setToX(scale);
        st.setToY(scale);
        st.play();
    }

    protected static Interpolator INTERPOLATOR_ANIMATE_CELL = new Interpolator() {
        @Override protected double curve(double t) {
            double s = 1.70158; t -= 1.0;
            return t * t * ((s + 1) * t + s) + 1.0;
        }
    };

    protected void animateCell(double targetHeight, double opacity) {
        if (expansionTimeline != null)
            expansionTimeline.stop();

        Interpolator googleBackOut = INTERPOLATOR_ANIMATE_CELL;

        expansionTimeline = new Timeline(
                new KeyFrame(Duration.millis(450),
                        new KeyValue(this.prefHeightProperty(), targetHeight, googleBackOut),
                        new KeyValue(pane.prefHeightProperty(), targetHeight, googleBackOut),
                        new KeyValue(extraInfoPane.opacityProperty(), opacity, Interpolator.EASE_OUT),
                        new KeyValue(extraInfoPane.translateYProperty(), opacity == 1 ? 0 : 12, googleBackOut)
                )
        );

        expansionTimeline.setOnFinished(e -> {
            if (getListView() != null) getListView().requestLayout();
        });

        expansionTimeline.play();
    }

    protected Node createExtraInfoContent() {
        return null;
    }

    protected abstract void onItemDropped(int draggedIndex, int targetIndex);
}