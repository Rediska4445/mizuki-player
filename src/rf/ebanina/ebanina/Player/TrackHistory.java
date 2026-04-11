package rf.ebanina.ebanina.Player;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import rf.ebanina.File.DataTypes;
import rf.ebanina.File.History;
import rf.ebanina.UI.Root;

import java.io.File;
import java.util.Iterator;
import java.util.Objects;

public class TrackHistory
        extends History<Track>
{
    protected final ContextMenu trackHistoryContextMenu;

    public TrackHistory(int maxSize, ContextMenu trackHistoryContextMenu) {
        super(maxSize);

        this.trackHistoryContextMenu = trackHistoryContextMenu;
    }

    @Override
    public void remove(Track track) {
        if(!history.contains(track)) {
            return;
        }

        super.remove(track);

        trackHistoryContextMenu.getItems().remove(createMenuItem(track));
    }

    @Override
    public void add(Track track) {
        if(history.contains(track)) {
            return;
        }

        super.add(track);

        trackHistoryContextMenu.getItems().add(createMenuItem(track));
    }

    protected void updateContextMenu() {
        Platform.runLater(() -> {
            trackHistoryContextMenu.getItems().clear();

            Iterator<Track> trackHistoryIterator = history.iterator();

            for (int line_number = 0; line_number < maxSize.get()
                    && trackHistoryIterator.hasNext(); line_number++) {
                Track t = trackHistoryIterator.next();

                MenuItem m = createMenuItem(t);

                trackHistoryContextMenu.getItems().add(m);
            }
        });
    }

    public MenuItem createMenuItem(Track track) {
        MenuItem m = new MenuItem(track.viewName() + " - " + track.getRawLastTimeTrack() + " - " + track.getState().get(DataTypes.LAST_DATE.code));
        m.setOnAction(e -> Root.PlaylistHandler.playlistHandler.openTrack(track));

        return m;
    }

    public void loadFromFile(File file) {
        super.loadFromFile(file, line -> {
            Track t = new Track(line);
            t.getLastTimeTrack();

            return t;
        });

        updateContextMenu();
    }

    public ContextMenu getTrackHistoryContextMenu() {
        return trackHistoryContextMenu;
    }

    @Override
    public int hashCode() {
        return Objects.hash(history, maxSize);
    }

    @Override
    public String toString() {
        return "TrackHistory{" +
                "history=" + history +
                '}';
    }
}