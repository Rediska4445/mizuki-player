package rf.ebanina.UI.UI.Paint;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public interface ColorPolicy {
    Color getColor(Image img);
    String getName();
}
