package rf.ebanina.UI.UI.Element.Controls;

import javafx.scene.layout.Region;

@FunctionalInterface
public interface CellFactory {
    Region createCell(int row, int col);
}