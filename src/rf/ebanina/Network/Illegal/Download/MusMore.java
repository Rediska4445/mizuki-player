package rf.ebanina.Network.Illegal.Download;

import javafx.scene.image.Image;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.Network.Info;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static rf.ebanina.UI.UI.Paint.ColorProcessor.isPreserveRatio;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isSmooth;

public class MusMore implements Info.IInfo {
    private final String mus_more_url = "https://ruo.morsmusic.org";

    @Override
    public Track getTrackDownloadLink(String track) {
        Track at = new Track();

        ArrayList<Track> tr = getMusMoreInfoTracks(track, "search");

        for (Track value : tr) {
            if (value.viewName.replace(" ", "").contains(track.replace(" ", ""))) {
                at = value;

                break;
            }
        }

        return at;
    }

    @Override
    public Track getTrackFromDownloadLink(String track) {
        Track at = new Track();

        ArrayList<Track> tr = getMusMoreInfoTracks(track, "search");

        for (Track value : tr) {
            if (value.viewName.replace(" ", "").contains(track.replace(" ", ""))) {
                at.setPath(value.toString());

                break;
            }
        }

        return at;
    }

    public ArrayList<rf.ebanina.ebanina.Player.Track> getMusMoreInfoTracks(String track, String addict) {
        ArrayList<rf.ebanina.ebanina.Player.Track> tr = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(mus_more_url + "/" + addict + "/" + URLEncoder.encode(track, StandardCharsets.UTF_8)).get();

            for (int i = 0; i < doc.getElementsByClass("track-control").size(); i++) {
                rf.ebanina.ebanina.Player.Track track1 = new rf.ebanina.ebanina.Player.Track(mus_more_url + doc.getElementsByClass("track-control").get(i).getElementsByClass("track-download").attr("href"));
                track1.artist = doc.getElementsByClass("track-info").get(i).getElementsByClass("media-link media-artist").text();
                track1.title = doc.getElementsByClass("track-info").get(i).getElementsByClass("media-link media-name").text();
                track1.viewName = track1.artist + " - " + track1.title;
                track1.setExternalUrl(Info.PlayersTypes.MUS_MORE.getCode());

                Image img = ResourceManager.Instance.loadImage(Info.PlayersTypes.MUS_MORE.getCode(), 40, 40, isPreserveRatio, isSmooth);
                track1.setMipmap(img);

                tr.add(track1);
            }
        } catch (IOException ignored) {
            throw new RuntimeException(ignored);
        }

        return tr;
    }
}
