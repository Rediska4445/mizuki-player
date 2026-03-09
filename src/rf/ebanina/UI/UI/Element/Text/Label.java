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

    public void setFontSize(int size) {
        super.setFont(ResourceManager.Instance.loadFont("main_font", size));
    }

    public void replace() {
        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> setLayouts(event.getSceneX() + getPrefWidth() / 2, event.getSceneY() + getPrefHeight() / 2));
    }

    public void setAnchorEvent(javafx.scene.Node node, int[] padding) {
        node.layoutXProperty().addListener((obs, oldVal, newVal) ->
            setLayoutX(node.getLayoutX() + padding[0])
        );

        node.layoutYProperty().addListener((obs, oldVal, newVal) ->
            setLayoutY(node.getLayoutY() + padding[1])
        );
    }

    public void showExternalBounds(String webColor) {
        // super.setStyle("-fx-border-color: " + webColor + ";");
    }

    public Label setColor(Color c) {
        setTextFill(c);

        return this;
    }

    public void updateColor(Color color) {
        super.setTextFill(color);
    }

    public void setLayouts(double x, double y) {
        super.setLayoutX(x);
        super.setLayoutY(y);
    }
}
