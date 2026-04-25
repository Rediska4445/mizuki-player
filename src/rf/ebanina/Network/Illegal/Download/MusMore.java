package rf.ebanina.Network.Illegal.Download;

import javafx.scene.image.Image;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.Network.Net;
import rf.ebanina.ebanina.Player.Track;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static rf.ebanina.UI.UI.Paint.ColorProcessor.isPreserveRatio;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isSmooth;

public class MusMore
        implements Net.IInfo
{
    protected static String mus_more_url = "https://ruo.morsmusic.org";

    @Override
    public Track getTrackDownloadLink(String track) {
        Track at = new Track();

        List<Track> tr = getTracksDownloadLinksList(track);

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

        List<Track> tr = getTracksDownloadLinksList(track);

        for (Track value : tr) {
            if (value.viewName.replace(" ", "").contains(track.replace(" ", ""))) {
                at.setPath(value.toString());

                break;
            }
        }

        return at;
    }

    @Override
    public List<Track> getTracksDownloadLinksList(String track) {
        ArrayList<rf.ebanina.ebanina.Player.Track> tr = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(mus_more_url + "/search/" + URLEncoder.encode(track, StandardCharsets.UTF_8))
                    .userAgent(Net.instance.getActiveUserAgent()).get();

            for (int i = 0; i < doc.getElementsByClass("track-control").size(); i++) {
                rf.ebanina.ebanina.Player.Track track1 = new rf.ebanina.ebanina.Player.Track(mus_more_url + doc.getElementsByClass("track-control").get(i).getElementsByClass("track-download").attr("href"));
                track1.setArtist(doc.getElementsByClass("track-info").get(i).getElementsByClass("media-link media-artist").text());
                track1.setTitle(doc.getElementsByClass("track-info").get(i).getElementsByClass("media-link media-name").text());
                track1.setViewName(track1.artist + " - " + track1.title);
                track1.setNetty(true);

                Image img = ResourceManager.Instance.loadImage(Net.PlayersTypes.MUS_MORE.getCode(), 40, 40, isPreserveRatio, isSmooth);
                track1.setMipmap(img);

                track1.putProperty(Track.Properties.EXTERNAL_URI, Net.PlayersTypes.MUS_MORE.getCode(), String.class);
                track1.putProperty(Track.Properties.MIPMAP_IS_LOADED, track1.mipmap != null, boolean.class);

                tr.add(track1);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return tr;
    }

    @Override
    public String toString() {
        return "MusMore{}";
    }
}
