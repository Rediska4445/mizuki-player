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

public class LightAudio
        implements Info.IInfo
{
    protected static final String light_audio_url = "https://web.ligaudio.ru/mp3/";

    @Override
    public Track getTrackDownloadLink(String track) {
        Track at = new Track();

        List<Track> tr = getTracksDownloadLinksList(URLEncoder.encode(track, StandardCharsets.UTF_8));

        for (Track value : tr) {
            if (value.viewName.replace(" ", "").equalsIgnoreCase(track.replace(" ", ""))) {
                at.setPath(value.getPath());
                at.setTotalDuraSec(value.getTotalDuraSec());
                return at;
            }
        }

        for (Track value : tr) {
            if (value.viewName.replace(" ", "").toLowerCase().contains(track.replace(" ", "").toLowerCase())) {
                at.setPath(value.getPath());
                at.setTotalDuraSec(value.getTotalDuraSec());
                return at;
            }
        }

        return at;
    }

    @Override
    public Track getTrackFromDownloadLink(String track) {
        Track at = new Track();

        List<Track> tr = getTracksDownloadLinksList(URLEncoder.encode(track, StandardCharsets.UTF_8));

        for (Track value : tr) {
            if (value.viewName.trim().replace(" ", "").equalsIgnoreCase(track.trim().replace(" ", ""))) {
                at
                        .setPath(value.toString())
                        .setMipmap(value.mipmap)
                        .setArtist(value.artist)
                        .setTitle(value.title)
                        .setTotalDuraSec(value.totalDuraSec);

                break;
            }
        }

        return at;
    }

    @Override
    public List<Track> getTracksDownloadLinksList(String c) {
        ArrayList<rf.ebanina.ebanina.Player.Track> res = new ArrayList<>();

        c = URLEncoder.encode(c, StandardCharsets.UTF_8);

        Music.mainLogger.info("getTracksDownloadLinksList: start for category/query = " + c);

        try {
            String currentUserAgent = Info.instance.getActiveUserAgent();
            Music.mainLogger.info("Using user-agent: " + currentUserAgent);

            int i3 = 1;
            int i2 = 0;

            String urlRoot = light_audio_url + c;
            Music.mainLogger.info("Fetching root page: " + urlRoot);

            Document doc = Jsoup.connect(urlRoot)
                    .userAgent(currentUserAgent)
                    .timeout(ConfigurationManager.instance.getIntItem("network_pre_download_timeout", "5000"))
                    .get();

            String linksUrl = light_audio_url + c + "/";
            Music.mainLogger.info("Fetching links block: " + linksUrl);

            Elements links = Jsoup.connect(linksUrl)
                    .userAgent(currentUserAgent)
                    .get()
                    .getElementById("result")
                    .getElementsByClass("item");

            int found = 0;
            try {
                found = Integer.parseInt(doc.getElementById("main")
                        .getElementsByClass("foundnum")
                        .text()
                        .replaceAll("\\D+", ""));
                Music.mainLogger.info("Found tracks count: " + found);
            } catch (Exception e) {
                Music.mainLogger.warn("Could not parse found tracks count, fallback to 0", e);
            }

            for (int i1 = 1; i1 < found; i1++) {
                if (i2 > 39) {
                    i2 = 1;
                    String pageUrl = light_audio_url + c + "/" + i3++;
                    Music.mainLogger.info("Fetching next page: " + pageUrl);

                    links = Jsoup.connect(pageUrl)
                            .userAgent(currentUserAgent)
                            .get()
                            .getElementById("result")
                            .getElementsByClass("item");
                }

                if (links.size() <= i2) {
                    Music.mainLogger.warn("No more links on page, breaking loop at i2=" + i2 + ", i1=" + i1);
                    break;
                }

                Element item = links.get(i2);

                String downHref = item.getElementsByClass("down").attr("href");
                String fullUrl = "https:" + downHref;

                rf.ebanina.ebanina.Player.Track tr = new rf.ebanina.ebanina.Player.Track(fullUrl);
                tr.setNetty(true);

                String title = item.getElementsByClass("title")
                        .get(0)
                        .getElementsByTag("span")
                        .get(0)
                        .text();
                String artist = item.getElementsByClass("title")
                        .get(0)
                        .getElementsByTag("span")
                        .get(1)
                        .text();
                String duraText = item.getElementsByClass("d").text();

                tr.setTitle(title);
                tr.setArtist(artist);
                tr.setTotalDuraSec(Track.getFormattedTotalDuration(duraText));
                Image img = ResourceManager.Instance.loadImage(Info.PlayersTypes.LIGHT_AUDIO.getCode(), 40, 40, isPreserveRatio, isSmooth);
                tr.setMipmap(img);
                tr.setViewName(tr.artist + " - " + tr.title.replace("-", ""));
                tr.setExternalUrl(Info.PlayersTypes.LIGHT_AUDIO.getCode());

                res.add(tr);
                i2++;
            }

            Music.mainLogger.info("getTracksDownloadLinksList: finished, tracks parsed: " + res.size());

        } catch (IOException e) {
            Music.mainLogger.severe("getTracksDownloadLinksList: network error for category/query = " + c, e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            Music.mainLogger.severe("getTracksDownloadLinksList: unexpected error for category/query = " + c, e);
            throw new RuntimeException(e);
        }

        return res;
    }

    @Override
    public String toString() {
        return "LightAudio{}";
    }
}
