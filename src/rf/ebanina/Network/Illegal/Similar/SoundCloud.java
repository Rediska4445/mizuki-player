package rf.ebanina.Network.Illegal.Similar;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.json.simple.parser.ParseException;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Root;
import rf.ebanina.Network.ISimilar;
import rf.ebanina.Network.Info;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static rf.ebanina.UI.Root.similar;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isPreserveRatio;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isSmooth;

public class SoundCloud implements ISimilar {
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
        rf.ebanina.Network.APIS.SoundCloud sc = new rf.ebanina.Network.APIS.SoundCloud();
        sc.relatedTracksCore.setLIMIT(ConfigurationManager.instance.getIntItem("soundcloud_related_limit", "25"));
        sc.searchCore.setLIMIT(1);

        List<rf.ebanina.Network.APIS.SoundCloudAPI.SoundCloudTrack> tr;

        try {
            tr = sc.getRelatedTrack(sc.Search(f));
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        ArrayList<Track> temp = new ArrayList<>();

        for(rf.ebanina.Network.APIS.SoundCloudAPI.SoundCloudTrack t : tr) {
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
