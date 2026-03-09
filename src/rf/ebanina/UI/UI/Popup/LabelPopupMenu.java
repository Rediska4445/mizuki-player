package rf.ebanina.UI.UI.Popup;

import javafx.scene.control.Label;

public class LabelPopupMenu extends PopupMenu {
    protected Label label;

    public LabelPopupMenu(String text) {
        label = new Label(
                text
        );

        label.setPrefWidth(100);
        label.setPrefHeight(100);

        getChildren().add(label);
    }

    public Label getLabel() {
        return label;
    }

    public LabelPopupMenu setLabel(Label label) {
        this.label = label;
        return this;
    }
}
