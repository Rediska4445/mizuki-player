package rf.ebanina.UI.UI.Element;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import rf.ebanina.File.History;
import rf.ebanina.File.Referencable;

import java.util.Iterator;

public class UiHistory<U extends Referencable>
        extends History<U>
{
    protected ContextMenu trackHistoryContextMenu;

    public UiHistory(int maxSize) {
        super(maxSize);
    }

    public UiHistory(int maxSize, ContextMenu trackHistoryContextMenu) {
        super(maxSize);

        this.trackHistoryContextMenu = trackHistoryContextMenu;
    }

    public MenuItem createMenuItem(U u) {
        return new MenuItem(u.path());
    }

    protected void updateContextMenu() {
        Platform.runLater(() -> {
            trackHistoryContextMenu.getItems().clear();

            Iterator<U> trackHistoryIterator = history.iterator();

            for (int line_number = 0; line_number < maxSize.get()
                    && trackHistoryIterator.hasNext(); line_number++) {
                U t = trackHistoryIterator.next();

                MenuItem m = createMenuItem(t);

                trackHistoryContextMenu.getItems().add(m);
            }
        });
    }

    @Override
    public void remove(U track) {
        super.remove(track);

        trackHistoryContextMenu.getItems().remove(createMenuItem(track));
    }

    @Override
    public void add(U track) {
        super.add(track);

        trackHistoryContextMenu.getItems().add(createMenuItem(track));
    }

    @Override
    public U back() {
        return super.back();
    }

    @Override
    public U forward() {
        return super.forward();
    }
}
