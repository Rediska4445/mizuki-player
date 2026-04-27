package rf.ebanina.ebanina.Player;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import rf.ebanina.File.DataTypes;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Element.UiHistory;

import java.io.File;
import java.util.Objects;

public class TrackHistory
        extends UiHistory<Track>
{
    public TrackHistory(int maxSize) {
        super(maxSize);
    }

    public TrackHistory(int maxSize, ContextMenu trackHistoryContextMenu) {
        super(maxSize, trackHistoryContextMenu);
    }

    @Override
    public MenuItem createMenuItem(Track track) {
        MenuItem m = new MenuItem(track.viewName() + " - " + track.getLastTimeTrack() + " - " + track.getState().get(DataTypes.LAST_DATE.code));
        m.setOnAction(e -> Root.PlaylistHandler.playlistHandler.openTrack(track));

        return m;
    }

    @Override
    public void remove(Track track) {
        if(!history.contains(track)) {
            return;
        }

        super.remove(track);
    }

    @Override
    public void add(Track track) {
        if(history.contains(track)) {
            return;
        }

        super.add(track);
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