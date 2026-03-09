package rf.ebanina.UI.UI.Context.Menu;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;

public class ContextMenuItem extends MenuItem {
    public ContextMenuItem() {
        super();
    }

    public ContextMenuItem(String s) {
        super(s);
    }

    public ContextMenuItem(Parent s) {
        super();

        setGraphic(s);
    }

    public ContextMenuItem(String s, Node node) {
        super(s, node);
    }
}

