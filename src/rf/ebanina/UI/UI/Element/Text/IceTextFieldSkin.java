package rf.ebanina.UI.UI.Element.Text;

import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

// TODO: Сделать анимацию смены цвета символов
// FIXME: При длинном тексте, при появлении новых символов в выделении - анимация ломается
public class IceTextFieldSkin
        extends TextFieldSkin
{
    private Node activeCaret = null;

    private final Timeline icePulse = new Timeline();

    private final Rectangle selectionRect = new Rectangle();
    private final DoubleProperty targetWidth = new SimpleDoubleProperty(0);
    private final DoubleProperty targetMinX = new SimpleDoubleProperty(0);
    private final DoubleProperty targetMaxX = new SimpleDoubleProperty(0);

    private boolean isFirstSelection = true;
    private boolean isFirstCaret = true;

    private double lastX = 0;

    public IceTextFieldSkin(TextField control) {
        super(control);

        icePulse.setAutoReverse(true);
        icePulse.setCycleCount(Animation.INDEFINITE);

        selectionRect.setManaged(false);
        selectionRect.setMouseTransparent(true);
        selectionRect.setArcWidth(10);
        selectionRect.setArcHeight(10);
        selectionRect.setOpacity(0);
        selectionRect.fillProperty().bind(ColorProcessor.core.mainClrProperty());

        targetWidth.addListener((obs, old, newValue) -> {
            Timeline t = new Timeline(new KeyFrame(Duration.millis(150),
                    new KeyValue(selectionRect.widthProperty(), newValue, Interpolator.EASE_OUT)));
            t.play();
        });

        ChangeListener<Number> boundaryListener = (obs, old, val) -> {
            Timeline t = new Timeline(new KeyFrame(Duration.millis(200),
                    new KeyValue(selectionRect.xProperty(), targetMinX.get(), Interpolator.EASE_OUT),
                    new KeyValue(selectionRect.widthProperty(), targetMaxX.get() - targetMinX.get(), Interpolator.EASE_OUT)
            ));
            t.play();
        };

        targetMinX.addListener(boundaryListener);
        targetMaxX.addListener(boundaryListener);

        control.setStyle(control.getStyle() + " -fx-highlight-fill: transparent;");
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);

        Node highlightNode = findHighlightNode(this.getNode());
        Node caretNode = findCaretNode(this.getNode());
        boolean hasSelection = getSkinnable().isFocused() && getSkinnable().getSelection().getLength() > 0;

        if (caretNode instanceof Path caret) {
            if (caret != activeCaret) {
                this.activeCaret = caret;
                setupCaretLogic(caret);
                isFirstCaret = true;
            }

            if (!caret.getElements().isEmpty() && caret.getElements().get(0) instanceof MoveTo mt) {
                double currentX = mt.getX();

                if (isFirstCaret) {
                    lastX = currentX;
                    caret.setTranslateX(0);
                    isFirstCaret = false;
                } else if (lastX != currentX && getSkinnable().isFocused()) {
                    double delta = lastX - currentX;
                    caret.setTranslateX(delta);

                    TranslateTransition move = new TranslateTransition(Duration.millis(200), caret);
                    move.setInterpolator(Root.iceInterpolator);
                    move.setToX(0);
                    move.play();

                    lastX = currentX;
                }
            }
        }

        if (highlightNode instanceof Path highlightPath && hasSelection) {
            highlightPath.setOpacity(0);
            if (!getChildren().contains(selectionRect)) getChildren().add(0, selectionRect);

            Bounds bounds = highlightPath.getBoundsInParent();

            if (isFirstSelection) {
                selectionRect.setX(bounds.getMinX());
                selectionRect.setWidth(bounds.getWidth());
                targetMinX.set(bounds.getMinX());
                targetMaxX.set(bounds.getMaxX());
                isFirstSelection = false;
            } else {
                targetMinX.set(bounds.getMinX());
                targetMaxX.set(bounds.getMaxX());
            }

            selectionRect.setY(bounds.getMinY());
            selectionRect.setHeight(bounds.getHeight());
            selectionRect.setVisible(true);
            selectionRect.setOpacity(1.0);
        } else {
            isFirstSelection = true;
            selectionRect.setOpacity(0);
            selectionRect.setVisible(false);
        }
    }

    private Node findHighlightNode(Node root) {
        if (root instanceof Parent p) {
            for (Node n : p.getChildrenUnmodifiable()) {
                if (n instanceof Path path && path.getElements().size() == 5) {
                    return n;
                }
                Node f = findHighlightNode(n);
                if (f != null) return f;
            }
        }

        return null;
    }

    private void setupCaretLogic(Path caret) {
        caret.visibleProperty().unbind();
        caret.opacityProperty().unbind();
        caret.strokeProperty().unbind();
        caret.strokeProperty().bind(ColorProcessor.core.mainClrProperty());

        caret.setVisible(true);
        caret.setStrokeWidth(1.5);

        icePulse.stop();
        icePulse.getKeyFrames().setAll(
                new KeyFrame(Duration.ZERO, new KeyValue(caret.opacityProperty(), 1.0, Interpolator.EASE_IN)),
                new KeyFrame(Duration.millis(750), new KeyValue(caret.opacityProperty(), 0.15, Interpolator.EASE_IN))
        );

        getSkinnable().focusedProperty().addListener((obs, old, isFocused) -> {
            if (isFocused) {
                caret.setOpacity(1);
                icePulse.play();
            } else {
                icePulse.stop();
                caret.setOpacity(0);
            }
        });

        if (getSkinnable().isFocused()) icePulse.play();
    }

    private Node findCaretNode(Node root) {
        if (root instanceof Parent parent) {
            for (Node node : parent.getChildrenUnmodifiable()) {
                if (node instanceof Group group) {
                    for (Node child : group.getChildren()) {
                        if (child instanceof Path) return child;
                    }
                }

                Node found = findCaretNode(node);
                if (found != null) return found;
            }
        }

        return null;
    }
}