package rf.ebanina.Network.Illegal.Download;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.Network.Info;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Track;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Hitmos
        implements Info.IInfo
{
    protected final String url = "https://rus.hitmotop.com";
    protected final String urlForDownload = "search?q=";

    @Override
    public Track getTrackDownloadLink(String track) {
        List<Track> trackList = getTracksDownloadLinksList(track, 1);

        if(trackList.size() > 0) {
            return trackList.get(0);
        }

        return new Track();
    }

    // TODO: Make it!
    @Override
    public Track getTrackFromDownloadLink(String track) {
        Track at = new Track();

        Document doc;

        try {
            doc = Jsoup.connect(url + "/" + urlForDownload + URLEncoder.encode(track, StandardCharsets.UTF_8)).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Elements media = doc.select("#pjax-container > div.content-inner > div > ul > li:nth-child(1) > div.track__info > div > a");

        for (Element src : media) {
            at.setPath(url + "/" + src.getElementsByTag("a").attr("href"));
            at.setTotalDuraSec(Integer.parseInt(src.select("div.track__fulltime").text()));
        }

        return at;
    }

    @Override
    public List<Track> getTracksDownloadLinksList(String track) {
        return getTracksDownloadLinksList(track, 35);
    }

    public List<Track> getTracksDownloadLinksList(String track, int max) {
        ArrayList<rf.ebanina.ebanina.Player.Track> A = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(url + "/" + urlForDownload + URLEncoder.encode(track, StandardCharsets.UTF_8))
                    .userAgent(Info.instance.getActiveUserAgent())
                    .timeout(ConfigurationManager.instance.getIntItem("network_pre_download_timeout", "5000"))
                    .get();

            for (int i = 0; i < max; i++) {
                Elements elements = doc.select("#pjax-container > div.content-inner > div > ul");

                for(Element track1 : elements) {
                    final String imgStyle = track1.select(".track__img").first().attr("style");

                    rf.ebanina.ebanina.Player.Track tr = new rf.ebanina.ebanina.Player.Track(url + track1.select("a.track__download-btn").attr("href"));
                    tr.title = track1.select(".track__title").first().text().trim();
                    tr.artist = track1.select(".track__desc").first().text().trim();
                    tr.setTotalDuraSec(Track.getFormattedTotalDuration(track1.select(".track__fulltime").first().text().trim()));
                    tr.viewName = tr.artist + " - " + tr.title;

                    final String imgUrl = imgStyle.replace("background-image: url('", "").replace("');", "");

                    tr.metadata.put("mipmap_is_loaded", true, boolean.class);

                    tr.setMipmap(Track.createMipmap(imgUrl));
                    tr.setExternalUrl(Info.PlayersTypes.HIT_MO.getCode());
                    tr.setNetty(true);

                    A.add(tr);
                }
            }
        } catch (IOException e) {
            Music.mainLogger.warn(e);
        }

        return A;
    }

    @Override
    public String toString() {
        return "Hitmos{}";
    }

    public static void main(String[] args) {
        Hitmos hitmos = new Hitmos();
        System.out.println(hitmos.getTrackDownloadLink("dvrst - close eyes"));
    }
}
