package rf.ebanina.UI.UI.Element.ListViews.Playlist;

import rf.ebanina.ebanina.Player.Track;

import java.util.List;

public interface Query<T extends Track> {
    String tag();
    List<T> search(String key, List<T> source);
}
