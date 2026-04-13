package rf.ebanina.Network.Illegal.Similar;

import javafx.application.Platform;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.Network.ISimilar;
import rf.ebanina.Network.Info;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.util.List;

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
            tr.mipmap = Track.createMipmap(ResourceManager.Instance.loadResource(Info.PlayersTypes.LASTFM.getCode()));
            tr.artist = t.getArtist();
            tr.title = t.getName();
            tr.totalDuraSec = t.getDuration();
            tr.viewName = tr.artist + " - " + tr.title;

            Platform.runLater(() -> {
                Root.rootImpl.similar.getTrackListView().getItems().add(tr);
                Root.PlaylistHandler.playlistHandler.playlistSimilar.add(tr);
                PlayProcessor.playProcessor.getTracks().addAll(Root.rootImpl.similar.getTrackListView().getItems());
            });
        }
    }

    @Override
    public List<Track> getSimilar(String f) {
        return List.of();
    }
}
