package rf.ebanina.Network;

import javafx.application.Platform;
import rf.ebanina.Network.Illegal.Download.Hitmos;
import rf.ebanina.Network.Illegal.Download.LightAudio;
import rf.ebanina.Network.Illegal.Download.MusMore;
import rf.ebanina.Network.Illegal.Similar.Apple;
import rf.ebanina.Network.Illegal.Similar.LastFM;
import rf.ebanina.Network.Illegal.Similar.SoundCloud;
import rf.ebanina.Network.Illegal.Similar.Spotify;
import rf.ebanina.ebanina.Player.Track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static rf.ebanina.UI.Root.similar;
import static rf.ebanina.UI.Root.tracksListView;

public class Info {
    public interface IInfo {
        Track getTrackDownloadLink(String track);
        Track getTrackFromDownloadLink(String track);
    }

    public static Info instance = new Info();

    private static final ExecutorService exec = Executors.newSingleThreadExecutor();

    private static volatile boolean isSimilarUpdateState = true;

    protected String activeAgentKey = "CHROME_WINDOWS";

    public final Map<String, String> USER_AGENTS = new HashMap<>() {{
        put("CHROME_WINDOWS", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        put("FIREFOX_WINDOWS", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0");
        put("SAFARI_MAC", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2.1 Safari/605.1.15");
    }};

    private static final String DEFAULT_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    private String cachedUserAgent = null;

    public String getActiveUserAgent() {
        if (cachedUserAgent != null) {
            return cachedUserAgent;
        }

        cachedUserAgent = USER_AGENTS.getOrDefault(activeAgentKey, DEFAULT_AGENT);
        return cachedUserAgent;
    }

    public static void similarStart() {
        isSimilarUpdateState = true;

        Platform.runLater(() -> similar.getTrackListView().getItems().clear());

        exec.submit(() -> updateSimilarList());
    }

    public static void similarStop() {
        isSimilarUpdateState = false;
    }

    public enum PlayersTypes {
        MUS_MORE("musmore"),
        LIGHT_AUDIO("lightaudio"),
        HIT_MO("hitmos"),
        APPLE("apple"),
        LASTFM("lastfm"),
        SPOTIFY("spotify"),
        SOUNDCLOUD("sound_cloud"),
        URI_NULL("uri_null");

        String code;

        public String getCode() {
            return code;
        }

        PlayersTypes(String code) {
            this.code = code;
        }
    }

    public static final Map<String, Info.IInfo> playersMap = new HashMap<>(Map.of(
            PlayersTypes.LIGHT_AUDIO.code, new LightAudio(),
            PlayersTypes.MUS_MORE.code, new MusMore(),
            PlayersTypes.HIT_MO.code, new Hitmos()
    ));

    public interface IIllegal {
        List<Track> getTrack(String in, String... any_other);
    }

    public static IIllegal getTracks = (in, any_other) -> {
        List<Track> res = new ArrayList<>();

        try {
            res.addAll(track_parse_get_info(in, Integer.parseInt(any_other[0])));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            res.addAll(getMusMoreInfoTracks(in, any_other[1]));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            res.addAll(getLightAudioInfoTracks(in));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    };

    public static List<ISimilar> similarList = List.of(
            new Spotify(),
            new LastFM(),
            new Apple(),
            new SoundCloud()
    );

    public static Runnable onUpdatedSimilarList = () -> {};

    public static ArrayList<rf.ebanina.ebanina.Player.Track> track_parse_get_info(String track, int max) {
        return ((Hitmos) playersMap.get(PlayersTypes.HIT_MO.code)).getTrackInfo(track, max);
    }

    public static ArrayList<rf.ebanina.ebanina.Player.Track> getMusMoreInfoTracks(String track, String addict) {
        return ((MusMore) playersMap.get(PlayersTypes.MUS_MORE.code)).getMusMoreInfoTracks(track, addict);
    }

    public static ArrayList<rf.ebanina.ebanina.Player.Track> getLightAudioInfoTracks(String c) {
        return ((LightAudio) playersMap.get(PlayersTypes.LIGHT_AUDIO.code)).getLightAudioInfoTracks(c);
    }

    public static void updateSimilarList() {
        if (similar.isVisible()) {
            String artist = tracksListView.getTrackListView().getSelectionModel().getSelectedItem().getArtist();
            String name = tracksListView.getTrackListView().getSelectionModel().getSelectedItem().getTitle();

            Platform.runLater(() -> similar.getCurrentPlaylistText().setText(artist + " - " + name));

            updateSimilarList(artist, name);
        }
    }

    public static void updateSimilarList(String author, String title) {
        updateSimilarList(
                new Track(Info.PlayersTypes.URI_NULL.getCode())
                .setViewName(author + " - " + title)
                .setArtist(author)
                .setTitle(title)
        );
    }

    public static Thread updateSimilarListAsync(String what) {
        return new Thread(() -> updateSimilarList(new Track(PlayersTypes.URI_NULL.getCode()).setViewName(what)));
    }

    public static void updateSimilarList(Track what) {
        if (similar.isVisible()) {
            Platform.runLater(() -> similar.getCurrentPlaylistText().setText(what.viewName()));

            for(ISimilar i : similarList) {
                if(!isSimilarUpdateState) {
                    break;
                }

                i.updateSimilar(what);
            }

            onUpdatedSimilarList.run();
        }
    }
}
