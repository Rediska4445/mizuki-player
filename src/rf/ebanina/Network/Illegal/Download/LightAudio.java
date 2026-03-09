package rf.ebanina.Network.Illegal.Download;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.Network.Info;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LightAudio implements Info.IInfo {
    private final String light_audio_url = "https://web.ligaudio.ru/mp3/";

    @Override
    public Track getTrackDownloadLink(String track) {
        Track at = new Track();

        ArrayList<Track> tr = getLightAudioInfoTracks(URLEncoder.encode(track, StandardCharsets.UTF_8));

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

        ArrayList<Track> tr = getLightAudioInfoTracks(URLEncoder.encode(track, StandardCharsets.UTF_8));

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

    public ArrayList<rf.ebanina.ebanina.Player.Track> getLightAudioInfoTracks(String c) {
        ArrayList<rf.ebanina.ebanina.Player.Track> res = new ArrayList<>();

        try {
            int i3 = 1;
            int i2 = 0;

            Document doc = Jsoup.connect(light_audio_url + c)
                    .timeout(ConfigurationManager.instance.getIntItem("network_pre_download_timeout", "5000"))
                    .get();

            Elements links = Jsoup.connect(light_audio_url + c + "/").get().getElementById("result").getElementsByClass("item");

            int found = 0;

            try {
                found = Integer.parseInt(doc.getElementById("main").getElementsByClass("foundnum").text().replaceAll("\\D+", ""));
            } catch (Exception e) {

            }

            for (int i1 = 1; i1 < found; i1++) {
                if (i2 > 39) {
                    i2 = 1;
                    links = Jsoup.connect(light_audio_url + c + "/" + i3++).get().getElementById("result").getElementsByClass("item");
                }

                rf.ebanina.ebanina.Player.Track tr = new rf.ebanina.ebanina.Player.Track("https:" + links.get(i2).getElementsByClass("down").attr("href"));
                tr.setNetty(true);

                tr.setTitle(links.get(i2).getElementsByClass("title")
                        .get(0).getElementsByTag("span")
                        .get(0).text());

                tr.setArtist(links.get(i2).getElementsByClass("title")
                        .get(0).getElementsByTag("span")
                        .get(1).text());

                tr.setTotalDuraSec(Track.getFormattedTotalDuration(links.get(i2).getElementsByClass("d")
                        .text()));

                tr.setViewName(tr.artist + " - " + tr.title.replace("-", ""));
                tr.setExternalUrl(Info.PlayersTypes.LIGHT_AUDIO.getCode());

//                if(links.get(i2).getElementsByTag("img").size() > 0) {
//                    Elements finalLinks = links;
//                    int finalI = i2;
//
//                    Platform.runLater(() -> tr.setMipmap(new Image("https:" + finalLinks.get(finalI).getElementsByTag("img").get(0).attr("src"), 40, 40, isPreserveRatio, isSmooth)));
//                } else {
//                    Platform.runLater(() -> tr.setMipmap(ResourceManager.Instance.loadImage(
//                            Info.PlayersTypes.LIGHT_AUDIO.getCode(), 40, 40, isPreserveRatio, isSmooth)
//                    ));
//                }

                res.add(tr);

                i2++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return res;
    }
}
