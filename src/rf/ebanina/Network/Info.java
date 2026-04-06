package rf.ebanina.Network;

import javafx.application.Platform;
import rf.ebanina.Network.Illegal.Download.Hitmos;
import rf.ebanina.Network.Illegal.Download.LightAudio;
import rf.ebanina.Network.Illegal.Download.MusMore;
import rf.ebanina.Network.Illegal.Similar.Apple;
import rf.ebanina.Network.Illegal.Similar.LastFM;
import rf.ebanina.Network.Illegal.Similar.SoundCloud;
import rf.ebanina.Network.Illegal.Similar.Spotify;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.utils.concurrency.LonelyThreadPool;
import rf.ebanina.utils.loggining.logging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@logging(tag = "NetworkHost/Info")
public class Info {
    public static Info instance = new Info();

    public interface IInfo {
        Track getTrackDownloadLink(String track);
        Track getTrackFromDownloadLink(String track);
        List<Track> getTracksDownloadLinksList(String track);
    }

    private final LonelyThreadPool exec = new LonelyThreadPool();

    private volatile boolean isSimilarUpdateState = true;

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

    public void similarStart() {
        isSimilarUpdateState = true;

        Platform.runLater(() -> Root.rootImpl.similar.getTrackListView().getItems().clear());

        exec.runNewTask(() -> Info.instance.updateSimilarList());
    }

    public void similarStop() {
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

    public static List<ISimilar> similarList = List.of(
            new Spotify(),
            new LastFM(),
            new Apple(),
            new SoundCloud()
    );

    public List<Track> getListOfTracks(String in, String... any_other) {
        List<Track> res = new ArrayList<>();

        for(Info.IInfo info : playersMap.values()) {
            Music.mainLogger.info("Now will be " + info);

            List<Track> tracks = null;

            try {
                tracks = info.getTracksDownloadLinksList(in);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

            Music.mainLogger.info(tracks);

            if(tracks != null) {
                res.addAll(tracks);
            }
        }

        Music.mainLogger.info("return");

        return res;
    }

    private Runnable onUpdatedSimilarList = () -> {};

    public Info setOnUpdatedSimilarList(Runnable onUpdatedSimilarList) {
        this.onUpdatedSimilarList = onUpdatedSimilarList;
        return this;
    }

    public void updateSimilarList() {
        if (Root.rootImpl.similar.isVisible()) {
            String artist = Root.rootImpl.tracksListView.getTrackListView().getSelectionModel().getSelectedItem().getArtist();
            String name = Root.rootImpl.tracksListView.getTrackListView().getSelectionModel().getSelectedItem().getTitle();

            Platform.runLater(() -> Root.rootImpl.similar.getCurrentPlaylistText().setText(artist + " - " + name));

            updateSimilarList(artist, name);
        }
    }

    public void updateSimilarList(String author, String title) {
        updateSimilarList(
                new Track(Info.PlayersTypes.URI_NULL.getCode())
                .setViewName(author + " - " + title)
                .setArtist(author)
                .setTitle(title)
        );
    }

    private final LonelyThreadPool updateSimilarListThread = new LonelyThreadPool();

    public void updateSimilarListAsync(String what) {
        updateSimilarListThread.runNewTask(() -> updateSimilarList(new Track(PlayersTypes.URI_NULL.getCode()).setViewName(what)));
    }

    public void updateSimilarList(Track what) {
        if (Root.rootImpl.similar.isVisible()) {
            Platform.runLater(() -> Root.rootImpl.similar.getCurrentPlaylistText().setText(what.viewName()));

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
