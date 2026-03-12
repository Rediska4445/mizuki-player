package rf.ebanina.UI.UI.Element.Text;

import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import rf.ebanina.File.Resources.ResourceManager;

public class Label extends javafx.scene.control.Label {
    public Label() {
        this(null);
    }

    public Label(String text) {
        this(text, Color.BLACK);
    }

    public Label(String text, Color color, double X, double Y) {
        super(text);
        super.setTextFill(color);
        super.setFont(ResourceManager.Instance.loadFont("main_font", 12));
        setLayouts(X, Y);
    }

    public Label(String text, Color color) {
        this(text, color, 0, 0);
    }

    public void replace() {
        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> setLayouts(event.getSceneX() + getPrefWidth() / 2, event.getSceneY() + getPrefHeight() / 2));
    }

    public Label setColor(Color c) {
        setTextFill(c);

        return this;
    }

    public void setLayouts(double x, double y) {
        super.setLayoutX(x);
        super.setLayoutY(y);
    }
}
