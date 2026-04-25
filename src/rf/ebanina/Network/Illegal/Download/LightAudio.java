package rf.ebanina.Network.Illegal.Download;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.Network.Net;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Track;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LightAudio
        implements Net.IInfo
{
    protected final String light_audio_url = "https://web.ligaudio.ru/mp3/";

    @Override
    public Track getTrackDownloadLink(String track) {
        List<Track> trackList = getTracksDownloadLinksList(track, 1);

        if(trackList.size() > 0) {
            return trackList.get(0);
        }

        return new Track();
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
        return getTracksDownloadLinksList(c, -1);
    }

    public List<Track> getTracksDownloadLinksList(String c, int max) {
        ArrayList<rf.ebanina.ebanina.Player.Track> res = new ArrayList<>();

        c = URLEncoder.encode(c, StandardCharsets.UTF_8);

        try {
            String currentUserAgent = Net.instance.getActiveUserAgent();

            int i3 = 1;
            int i2 = 0;

            String urlRoot = light_audio_url + c;

            Document doc = Jsoup.connect(urlRoot)
                    .userAgent(currentUserAgent)
                    .timeout(ConfigurationManager.instance.getIntItem("network_pre_download_timeout", "5000"))
                    .get();

            String linksUrl = light_audio_url + c + "/";

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
            } catch (Exception e) {
                Music.mainLogger.warn("Could not parse found tracks count, fallback to 0", e);
            }

            max += 1;
            max = max < 0 ? found : max;

            for (int i1 = 1; i1 < max; i1++) {
                if (i2 > 39) {
                    i2 = 1;
                    String pageUrl = light_audio_url + c + "/" + i3++;

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

                tr.putProperty(Track.Properties.EXTERNAL_URI, Net.PlayersTypes.LIGHT_AUDIO.getCode(), String.class);
                tr.putProperty(Track.Properties.MIPMAP_IS_LOADED, true, boolean.class);

                tr.setTitle(title);
                tr.setArtist(artist);
                tr.setTotalDuraSec(Track.getFormattedTotalDuration(duraText));
                tr.setViewName(tr.artist + " - " + tr.title.replace("-", ""));

                res.add(tr);
                i2++;
            }
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
        return "LightAudio{" +
                "light_audio_url='" + light_audio_url + '\'' +
                '}';
    }

    public static void main(String[] args) {
        LightAudio lightAudio = new LightAudio();
        System.out.println(lightAudio.getTrackDownloadLink("dvrst - close eyes"));
    }
}
