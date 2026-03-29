package rf.ebanina.Network.Illegal.Similar;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.json.simple.parser.ParseException;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.Network.ISimilar;
import rf.ebanina.Network.Info;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static rf.ebanina.UI.Root.similar;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isPreserveRatio;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isSmooth;

public class SoundCloud
        implements ISimilar
{
    public static String SOUNDCLOUD_CLIENT_ID;

    public void initClientId() {
        SOUNDCLOUD_CLIENT_ID = "client_id=" + ConfigurationManager.instance.getItem("soundcloud_api_client_id", "");

        if(ConfigurationManager.instance.getItem("soundcloud_api_client_id", "").equalsIgnoreCase("null")) {
            try {
                SOUNDCLOUD_CLIENT_ID = updateClientId();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String updateClientId() throws IOException {
        String clientId;

        soundcloud.network.Request request = new soundcloud.network.Request(new URL("https://a-v2.sndcdn.com/assets/0-cbfed2d8.js"));
        soundcloud.network.Response response = request.send();

        String body = response.getBody().toString();
        String clientIdName = "client_id=";
        int clientIdIndex = body.indexOf(clientIdName);

        clientId = body.substring(clientIdIndex + clientIdName.length(), body.indexOf("\"", clientIdIndex));

        return "client_id=" + clientId;
    }

    @Override
    public void updateSimilar(Track track) {
        ArrayList<Track> temp = new ArrayList<>(getSimilar(track.viewName()));

        if (temp.size() != 0) {
            Platform.runLater(() -> {
                similar.getTrackListView().getItems().addAll(temp);
                Root.PlaylistHandler.playlistSimilar.addAll(temp);
                PlayProcessor.playProcessor.getTracks().addAll(similar.getTrackListView().getItems());
            });
        }
    }

    @Override
    public List<Track> getSimilar(String f) {
        if(SOUNDCLOUD_CLIENT_ID == null) {
            initClientId();
        }

        soundcloud.SoundCloud sc = new soundcloud.SoundCloud(SOUNDCLOUD_CLIENT_ID);
        sc.limit = (ConfigurationManager.instance.getIntItem("soundcloud_related_limit", "25"));

        List<soundcloud.api.SoundCloudTrack> tr;

        try {
            tr = sc.getRelatedTracks(sc.Search(f));
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        ArrayList<Track> temp = new ArrayList<>();

        for(soundcloud.api.SoundCloudTrack t : tr) {
            temp.add(
                    new Track(Info.PlayersTypes.URI_NULL.getCode())
                            .setTitle(t.getTitle())
                            .setArtist(t.getArtist())
                            .setViewName(t.getArtist() + " - " + t.getTitle())
                            .setExternalUrl("sound_cloud")
                            .setTotalDuraSec((int) Float.parseFloat(t.getDura()) / 1000)
                            .setMipmap(ResourceManager.Instance.loadImage(Info.PlayersTypes.SOUNDCLOUD.getCode(), 40, 40, isPreserveRatio, isSmooth))
                            .setViewName(t.getArtist() + " - " + t.getTitle())
                            .setAlbumArt(new Image(t.getArtwork()))
                            .setNetty(true)
            );
        }

        return temp;
    }
}
