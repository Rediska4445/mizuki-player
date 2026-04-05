package rf.ebanina.Network.Illegal.Similar;

import javafx.application.Platform;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.Network.ISimilar;
import rf.ebanina.Network.Info;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.util.List;

import static rf.ebanina.UI.Root.similar;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isPreserveRatio;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isSmooth;

// FIXME: Не работает блять! (из рф)
public class LastFM
        implements ISimilar
{
    public static final String LASTFM_API_KEY = "52ad98fbc4ea0816d852a1ce0598b740";

    @Override
    public void updateSimilar(Track track) {
        for (de.umass.lastfm.Track t : de.umass.lastfm.Track.getSimilar(track.artist, track.title, LASTFM_API_KEY)) {
            Track tr = new Track(Info.PlayersTypes.URI_NULL.getCode());
            tr.setExternalUrl(Info.PlayersTypes.LASTFM.getCode());
            tr.mipmap = ResourceManager.Instance.loadImage(Info.PlayersTypes.LASTFM.getCode(), 40, 40, isPreserveRatio, isSmooth);
            tr.artist = t.getArtist();
            tr.title = t.getName();
            tr.totalDuraSec = t.getDuration();
            tr.viewName = tr.artist + " - " + tr.title;

            Platform.runLater(() -> {
                similar.getTrackListView().getItems().add(tr);
                Root.PlaylistHandler.playlistSimilar.add(tr);
                PlayProcessor.playProcessor.getTracks().addAll(similar.getTrackListView().getItems());
            });
        }
    }

    @Override
    public List<Track> getSimilar(String f) {
        return List.of();
    }
}
