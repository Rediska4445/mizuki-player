package rf.ebanina.UI.Editors;

import javafx.scene.Parent;

import java.io.IOException;

public interface IViewable
{
    Parent parent() throws IOException;

    String name();

    String description();
}
