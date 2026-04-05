package rf.ebanina.Network.Illegal.Download;

import javafx.scene.image.Image;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.Network.Info;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Track;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static rf.ebanina.UI.UI.Paint.ColorProcessor.isPreserveRatio;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isSmooth;

public class Hitmos
        implements Info.IInfo
{
    protected static final String urlForDownload = "https://rus.hitmotop.com/search?q=";

    @Override
    public Track getTrackDownloadLink(String track) {
        Track at = new Track();

        Document doc;

        try {
            doc = Jsoup.connect(urlForDownload + URLEncoder.encode(track, StandardCharsets.UTF_8)).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Elements media = doc.select("#pjax-container > div.content-inner > div > ul > li:nth-child(1) > div.track__info > div > a");

        for (Element src : media) {
            at.setPath(src.getElementsByTag("a").attr("href"));
        }

        //FIXME: парсит общее время
        at.setTotalDuraSec(125);

        return at;
    }

    @Override
    public Track getTrackFromDownloadLink(String track) {
        Track at = new Track();

        Document doc;

        try {
            doc = Jsoup.connect(urlForDownload + URLEncoder.encode(track, StandardCharsets.UTF_8)).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Elements media = doc.select("#pjax-container > div.content-inner > div > ul > li:nth-child(1) > div.track__info > div > a");

        for (Element src : media) {
            at.setPath(src.getElementsByTag("a").attr("href"));

            at.setTotalDuraSec(Integer.parseInt(src.select("div.track__fulltime").text()));
        }

        return at;
    }

    @Override
    public List<Track> getTracksDownloadLinksList(String track) {
        ArrayList<rf.ebanina.ebanina.Player.Track> A = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(urlForDownload + URLEncoder.encode(track, StandardCharsets.UTF_8))
                    .userAgent(Info.instance.getActiveUserAgent())
                    .timeout(ConfigurationManager.instance.getIntItem("network_pre_download_timeout", "5000"))
                    .get();

            for (int i = 0; i < 50; i++) {
                for (Element src : doc.select("#pjax-container > div.content-inner > div > ul > li:nth-child(" + i + ") > div.track__info > a")) {
                    rf.ebanina.ebanina.Player.Track tr = new rf.ebanina.ebanina.Player.Track(
                            doc.select("#pjax-container > div.content-inner > div > ul > li:nth-child(" + i + ") > div.track__info > div > a")
                                    .get(0).getElementsByTag("a").attr("href"));
                    tr.title = src.select("div.track__title").text();
                    tr.artist = src.select("div.track__desc").text();
                    tr.viewName = tr.artist + " - " + tr.title;

                    Image img = ResourceManager.Instance.loadImage(Info.PlayersTypes.HIT_MO.getCode(), 40, 40, isPreserveRatio, isSmooth);

                    tr.setMipmap(img);
                    tr.setExternalUrl(Info.PlayersTypes.HIT_MO.getCode());
                    tr.setNetty(true);

                    A.add(tr);
                }
            }
        } catch (IOException e) {
            Music.mainLogger.warn(e);
        }

        Music.mainLogger.println(A);

        return A;
    }

    @Override
    public String toString() {
        return "Hitmos{}";
    }
}
