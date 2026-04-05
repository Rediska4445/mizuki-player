package rf.ebanina.UI.UI.Element.ListViews;

import javafx.scene.control.ListView;
import javafx.scene.control.skin.VirtualFlow;

public class ListViewSkin<T> extends javafx.scene.control.skin.ListViewSkin<T> {
    public ListViewSkin(ListView<T> listView) {
        super(listView);

        VirtualFlow<?> flow = (VirtualFlow<?>) getChildren().get(0);
        flow.setSnapToPixel(true);
        flow.setCellCount(getSkinnable().getItems().size());
    }
}
