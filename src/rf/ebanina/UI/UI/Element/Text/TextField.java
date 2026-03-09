package rf.ebanina.UI.UI.Element.Text;

import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.util.concurrent.atomic.AtomicReference;

public class TextField extends JFXTextField {
    private ObjectProperty<Color> colorProperty;
    private ChangeListener<Color> colorListener;

    public TextField() {
        this("");
    }

    public TextField(String text) {
        super(text);

        setFontSize(11);
        setAlignment(Pos.CENTER);
        visibleFocus(true);

        colorProperty = new SimpleObjectProperty<>();
        colorProperty.addListener(colorListener = (obs, oldVal, newVal) -> {
            Platform.runLater(() -> updateColor(newVal));
        });

        textProperty().addListener((obs, oldText, newText) -> updateAlignment());
        widthProperty().addListener((obs, oldWidth, newWidth) -> updateAlignment());
    }

    private void updateAlignment() {
        Platform.runLater(() -> {
            String text = getText();
            if (text == null || text.isEmpty()) {
                setAlignment(Pos.CENTER);
                return;
            }

            Text tempText = new Text(text);
            tempText.setFont(getFont());
            double textWidth = tempText.getLayoutBounds().getWidth();
            double fieldWidth = getWidth() - getPadding().getLeft() - getPadding().getRight();

            setAlignment(textWidth <= fieldWidth ? Pos.CENTER : Pos.CENTER_LEFT);
        });
    }

    public ObjectProperty<Color> getColorProperty() {
        return colorProperty;
    }

    public Color getCurrentColor() {
        AtomicReference<Color> res = new AtomicReference<>(Color.BLACK);

        Platform.runLater(() -> {
            Node textNode = lookup(".text");

            if (textNode != null) {
                if (textNode instanceof Text text) {
                    if (text.getFill() instanceof Color) {
                        res.set((Color) text.getFill());
                    }
                }
            } else {
                throw new NullPointerException("lookup(\".text\") not can be null");
            }
        });

        return res.get();
    }

    public void visibleFocus(boolean visible) {
        setBackground(Background.EMPTY);

        if(visible) {
            setUnFocusColor(Color.TRANSPARENT);
            setFocusColor(Color.TRANSPARENT);
        } else {
            setUnFocusColor(ColorProcessor.core.getMainClr());
            setFocusColor(Color.TRANSPARENT);
        }
    }

    public void setFontSize(int size) {
        super.setFont(ResourceManager.Instance.loadFont("main_font", size));
    }

    public void replace() {
        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> setLayouts(event.getSceneX() + getPrefWidth() / 2, event.getSceneY() + getPrefHeight() / 2));
    }

    public void updateColor(Color color) {
        if (color == null)
            return;

        String colorHex = ColorProcessor.core.toHex(color);

        Platform.runLater(() -> {
            Node textNode = lookup(".text");

            if(textNode != null) {
                if (textNode instanceof Text text) {
                    if (text.getFill() instanceof Color) {
                        if(text.fillProperty().isBound()) {
                            text.fillProperty().unbind();
                        }

                        text.setFill(Color.valueOf(colorHex));
                    }
                }
            } else {
                throw new NullPointerException("lookup(\".text\") not can be null");
            }
        });
    }

    public void setLayouts(double x, double y) {
        setLayoutX(x);
        setLayoutY(y);
    }
}
