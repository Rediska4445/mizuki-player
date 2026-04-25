package rf.ebanina.Network.Illegal.Similar;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.json.simple.parser.ParseException;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.Network.ISimilar;
import rf.ebanina.Network.Net;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static rf.ebanina.UI.UI.Paint.ColorProcessor.isPreserveRatio;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isSmooth;

// FIXME: Не работает блять! (из рф)
public class SoundCloud
        implements ISimilar
{
    public static soundcloud.SoundCloud sc;

    public SoundCloud() {
        if(sc == null) {
            initSoundCloudAPI();

            sc.limit = (ConfigurationManager.instance.getIntItem("soundcloud_related_limit", "25"));
        }
    }

    public void initSoundCloudAPI() {
        String SOUNDCLOUD_CLIENT_ID = "client_id=" + ConfigurationManager.instance.getItem("soundcloud_api_client_id", "");

        sc = new soundcloud.SoundCloud(SOUNDCLOUD_CLIENT_ID);

        if(ConfigurationManager.instance.getItem("soundcloud_api_client_id", "").equalsIgnoreCase("null")) {
            try {
                SOUNDCLOUD_CLIENT_ID = sc.updateClientId();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void updateSimilar(Track track) {
        ArrayList<Track> temp = new ArrayList<>(getSimilar(track.viewName()));

        if (temp.size() != 0) {
            Platform.runLater(() -> {
                Root.rootImpl.similar.getTrackListView().getItems().addAll(temp);
                Root.PlaylistHandler.playlistHandler.playlistSimilar.addAll(temp);
                PlayProcessor.playProcessor.getTracks().addAll(Root.rootImpl.similar.getTrackListView().getItems());
            });
        }
    }

    @Override
    public List<Track> getSimilar(String f) {
        List<soundcloud.api.SoundCloudTrack> tr;

        try {
            tr = sc.getRelatedTracks(sc.Search(f));
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        ArrayList<Track> temp = new ArrayList<>();

        for(soundcloud.api.SoundCloudTrack t : tr) {
            temp.add(
                    new Track(Net.PlayersTypes.URI_NULL.getCode())
                            .setTitle(t.getTitle())
                            .setArtist(t.getArtist())
                            .setViewName(t.getArtist() + " - " + t.getTitle())
                            .putProperty(Track.Properties.EXTERNAL_URI, "sound_cloud", String.class)
                            .setTotalDuraSec((int) Float.parseFloat(t.getDura()) / 1000)
                            .setMipmap(ResourceManager.Instance.loadImage(Net.PlayersTypes.SOUNDCLOUD.getCode(), 40, 40, isPreserveRatio, isSmooth))
                            .setViewName(t.getArtist() + " - " + t.getTitle())
                            .setAlbumArt(new Image(t.getArtwork()))
                            .setNetty(true)
            );
        }

        return temp;
    }
}
