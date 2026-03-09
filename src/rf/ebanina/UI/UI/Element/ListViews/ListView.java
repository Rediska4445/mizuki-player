package rf.ebanina.UI.UI.Element.ListViews;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.AnimatedListCell;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import static rf.ebanina.utils.Math.clamp;

public class ListView<T> extends javafx.scene.control.ListView<T> {
    private ObjectProperty<Color> selectedColorProperty = new SimpleObjectProperty<>(ColorProcessor.core.getMainClr());

    public ObjectProperty<Color> getSelectedColorProperty() {
        return selectedColorProperty;
    }

    public ObjectProperty<Color> selectedColorPropertyProperty() {
        return selectedColorProperty;
    }

    public void setSelectedColorProperty(ObjectProperty<Color> selectedColorProperty) {
        this.selectedColorProperty = selectedColorProperty;
    }

    public void setSelectedColor(Color selectedColor) {
        selectedColorProperty.set(selectedColor);
    }

    public Color getSelectedColor() {
        return selectedColorProperty.get();
    }

    public void updateBorderColor(Color color) {
        setStyle(null);
        setStyle(
                "-fx-border-color: " + ColorProcessor.core.toHex(color) + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-style: solid;" +
                        "-fx-border-radius: 4;" +
                        "-fx-border-insets: 0;"
        );
    }

    public void updateSelectedBackground(Color newColor) {
        setSelectedColor(newColor);

        for (Node node : lookupAll(".list-cell")) {
            if (node instanceof ListCell<?> cell) {
                if (cell.isSelected()) {
                    Border newBorder = AnimatedListCell.borderFactory(newColor);

                    cell.setBorder(newBorder);
                } else {
                    cell.setBorder(null);
                }
            }
        }
    }

    public ListView() {
        this(FXCollections.observableArrayList());
    }

    public ListView(ObservableList<T> tracks) {
        super(tracks);

        prepareToScrollAnimation();

        addSmoothScroll();
        addSmoothHighlightOnScrollBars();
    }

    private ScrollBar vScrollBar;
    private Timeline timeline;
    private double sensitivity = 0.25;

    private double targetValue = 0;

    private boolean userIsDragging = false;

    public void addSmoothScroll() {
        addEventFilter(ScrollEvent.SCROLL, smoothScrollEventHandler);
    }

    public void removeSmoothScroll() {
        removeEventFilter(ScrollEvent.SCROLL, smoothScrollEventHandler);
    }

    private void prepareToScrollAnimation() {
        Platform.runLater(() -> {
            vScrollBar = findScrollBar(this, Orientation.VERTICAL);

            if (vScrollBar == null) {
                System.err.println("Vertical ScrollBar not found!");
                return;
            }

            targetValue = vScrollBar.getValue();

            makeScrollBarAlwaysVisibleAndStyled(vScrollBar);

            vScrollBar.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                userIsDragging = true;
                if (timeline != null) {
                    timeline.stop();
                }
            });

            vScrollBar.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
                userIsDragging = false;
                targetValue = vScrollBar.getValue();
            });

            vScrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (userIsDragging) {
                    targetValue = newVal.doubleValue();
                }
            });
        });
    }

    public void addSmoothHighlightOnScrollBars() {
        Platform.runLater(() -> {
            getStylesheets().add(ResourceManager.Instance.loadStylesheet("scrollbar-fixed-width"));

            for (Node node : lookupAll(".scroll-bar")) {
                if (node instanceof ScrollBar sb && sb.getOrientation() == Orientation.HORIZONTAL) {
                    sb.setVisible(true);
                    sb.setManaged(true);
                    sb.setOpacity(1);

                    if (sb.getMax() == 0.0)
                        sb.setMax(1.0);
                }
            }

            ScrollPane scrollPane = (ScrollPane) lookup(".scroll-pane");

            if (scrollPane != null) {
                scrollPane.setVisible(true);
                scrollPane.setManaged(true);
                scrollPane.setOpacity(1.0);
                scrollPane.setPrefHeight(15);
                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            }

            ScrollBar vScrollBar = findScrollBar(this, Orientation.VERTICAL);
            ScrollBar hScrollBar = findScrollBar(this, Orientation.HORIZONTAL);

            if (vScrollBar != null) {
                setupFadeAndPressTransition(vScrollBar);
            }
            if (hScrollBar != null) {
                setupFadeAndPressTransition(hScrollBar);
            }
        });
    }

    private void setupFadeAndPressTransition(ScrollBar scrollBar) {
        scrollBar.setOpacity(0.3);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), scrollBar);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), scrollBar);
        fadeOut.setToValue(0.3);

        FadeTransition fadePress = new FadeTransition(Duration.millis(100), scrollBar);
        fadePress.setToValue(1.0);

        scrollBar.setOnMouseEntered(e -> {
            fadeOut.stop();
            fadeIn.playFromStart();
        });

        scrollBar.setOnMouseExited(e -> {
            fadeIn.stop();
            fadeOut.playFromStart();
        });

        scrollBar.setOnMousePressed(e -> {
            fadeOut.stop();
            fadeIn.stop();
            fadePress.playFromStart();
        });

        scrollBar.setOnMouseReleased(e -> {
            fadePress.stop();
            fadeOut.playFromStart();
        });
    }

    private EventHandler<? super ScrollEvent> smoothScrollEventHandler = new EventHandler<>() {
        @Override
        public void handle(ScrollEvent event) {
            event.consume();

            if (userIsDragging) {
                return;
            }

            int cellCount = getItems().size();
            double delta = event.getDeltaY() * sensitivity * (1.0 / java.lang.Math.max(1, cellCount));

            targetValue = clamp(targetValue - delta, vScrollBar.getMin(), vScrollBar.getMax());


            if (timeline == null || timeline.getStatus() != Animation.Status.RUNNING) {
                timeline = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(vScrollBar.valueProperty(), vScrollBar.getValue())),
                        new KeyFrame(Duration.millis(600), new KeyValue(vScrollBar.valueProperty(), targetValue, Interpolator.SPLINE(0.25, 0.1, 0.25, 1)))
                );

                timeline.play();
            } else {
                KeyValue kv = new KeyValue(vScrollBar.valueProperty(), targetValue, Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
                KeyFrame kf = new KeyFrame(Duration.millis(600), kv);

                timeline.stop();
                timeline.getKeyFrames().setAll(
                        new KeyFrame(Duration.ZERO, new KeyValue(vScrollBar.valueProperty(), vScrollBar.getValue())),
                        kf
                );

                timeline.playFromStart();
            }
        }
    };

    private ScrollBar findScrollBar(javafx.scene.control.ListView<?> listView, Orientation orientation) {
        for (var node : listView.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar sb && sb.getOrientation() == orientation) {
                return sb;
            }
        }

        return null;
    }

    private void makeScrollBarAlwaysVisibleAndStyled(ScrollBar scrollBar) {
        scrollBar.setVisible(true);
        scrollBar.setOpacity(0.5);
        scrollBar.setMouseTransparent(false);
        scrollBar.setPadding(new Insets(0));
        scrollBar.setBackground(new Background(new BackgroundFill(
                Color.TRANSPARENT,
                new CornerRadii(5),
                new Insets(0,0,0,0))
        ));
    }
}
