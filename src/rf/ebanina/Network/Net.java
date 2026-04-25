package rf.ebanina.Network;

import javafx.application.Platform;
import rf.ebanina.Network.Illegal.Download.Hitmos;
import rf.ebanina.Network.Illegal.Download.LightAudio;
import rf.ebanina.Network.Illegal.Download.MusMore;
import rf.ebanina.Network.Illegal.Similar.*;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.utils.concurrency.LonelyThreadPool;
import rf.ebanina.utils.loggining.ILogging;
import rf.ebanina.utils.loggining.logging;
import rf.ebanina.utils.network.UserAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@logging(tag = "NetworkHost/Net")
public class Net {
    public static Net instance = defaultInstance();

    public Net(ILogging logger, Root rootImpl) {
        this.logger = logger;
        this.rootImpl = rootImpl;
    }

    public static Net getInstance() {
        if(instance == null)
            return instance = defaultInstance();

        return instance;
    }

    public static Net defaultInstance() {
        return new Net(Music.mainLogger, Root.rootImpl);
    }

    public interface IInfo {
        Track getTrackDownloadLink(String track);
        Track getTrackFromDownloadLink(String track);
        List<Track> getTracksDownloadLinksList(String track);
    }

    private final ILogging logger;

    private final Root rootImpl;

    private static final String DEFAULT_AGENT = UserAgent.WINDOWS_CHROME.getCode();

    protected String activeAgentKey = "CHROME_WINDOWS";
    private String cachedUserAgent = null;

    private final LonelyThreadPool similarThreadPool = new LonelyThreadPool();
    private volatile boolean isSimilarUpdateState = true;

    public final Map<String, String> USER_AGENTS = new HashMap<>() {{
        put("CHROME_WINDOWS", UserAgent.WINDOWS_CHROME.getCode());
        put("FIREFOX_WINDOWS", UserAgent.WINDOWS_FIREFOX.getCode());
        put("SAFARI_MAC", UserAgent.MACOS_SAFARI.getCode());
    }};


    public static final Map<String, Net.IInfo> playersMap = new HashMap<>(Map.of(
            PlayersTypes.LIGHT_AUDIO.code, new LightAudio(),
            PlayersTypes.MUS_MORE.code, new MusMore(),
            PlayersTypes.HIT_MO.code, new Hitmos()
    ));

    public static Map<String, ISimilar> similarMap = new HashMap<>(Map.of(
            PlayersTypes.SPOTIFY.code, new Spotify(),
            PlayersTypes.DEEZER.code, new Deezer(),
            PlayersTypes.LASTFM.code, new LastFM(),
            PlayersTypes.APPLE.code, new Apple(),
            PlayersTypes.SOUNDCLOUD.code, new SoundCloud()
    ));

    public Map<String, ISimilar> getSimilarMap() {
        return similarMap;
    }

    public String getActiveUserAgent() {
        if (cachedUserAgent != null) {
            return cachedUserAgent;
        }

        cachedUserAgent = USER_AGENTS.getOrDefault(activeAgentKey, DEFAULT_AGENT);

        return cachedUserAgent;
    }

    public void similarStart() {
        isSimilarUpdateState = true;

        Platform.runLater(() -> rootImpl.similar.getTrackListView().getItems().clear());

        similarThreadPool.runNewTask(this::updateSimilarList);
    }

    public void similarStop() {
        isSimilarUpdateState = false;
    }
    public enum PlayersTypes {
        MUS_MORE("musmore", "MusMore"),
        LIGHT_AUDIO("lightaudio", "LightAudio"),
        HIT_MO("hitmos", "HitMos"),
        APPLE("apple", "Apple Music"),
        LASTFM("lastfm", "LastFM"),
        SPOTIFY("spotify", "Spotify"),
        SOUNDCLOUD("sound_cloud", "SoundCloud"),
        DEEZER("deezer", "Deezer"),
        URI_NULL("uri_null", "Uri Null");

        String code;
        String name;


        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
        PlayersTypes(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    public List<Track> getListOfTracks(String in, String... any_other) {
        List<Track> res = new ArrayList<>();

        for(Net.IInfo info : playersMap.values()) {
            List<Track> tracks = null;

            try {
                tracks = info.getTracksDownloadLinksList(in);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

            if(tracks != null) {
                res.addAll(tracks);
            }
        }

        return res;
    }

    private Runnable onUpdatedSimilarList = () -> {};

    public Net setOnUpdatedSimilarList(Runnable onUpdatedSimilarList) {
        this.onUpdatedSimilarList = onUpdatedSimilarList;
        return this;
    }

    public void updateSimilarList() {
        if (rootImpl.similar.isVisible()) {
            String artist = rootImpl.tracksListView.getTrackListView().getSelectionModel().getSelectedItem().getArtist();
            String name = rootImpl.tracksListView.getTrackListView().getSelectionModel().getSelectedItem().getTitle();

            updateSimilarList(artist, name);
        }
    }

    public void updateSimilarList(String author, String title) {
        updateSimilarList(
                new Track(Net.PlayersTypes.URI_NULL.getCode())
                .setViewName(author + " - " + title)
                .setArtist(author)
                .setTitle(title)
        );
    }

    public void updateSimilarListAsync(String what) {
        similarThreadPool.runNewTask(() -> updateSimilarList(new Track(PlayersTypes.URI_NULL.getCode()).setViewName(what)));
    }

    private final List<LonelyThreadPool> threadPools = new ArrayList<>();

    public void updateSimilarList(Track what) {
        Platform.runLater(() -> rootImpl.similar.getCurrentPlaylistText().setText(what.viewName()));

        for (LonelyThreadPool lonelyThreadPool : threadPools) {
            try {
                lonelyThreadPool.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        threadPools.clear();

        for(ISimilar i : similarMap.values()) {
            if(!isSimilarUpdateState) {
                break;
            }

            LonelyThreadPool threadPool = new LonelyThreadPool();
            threadPool.runNewTask(() -> i.updateSimilar(what));

            logger.info("Now is: " + i.toString());

            threadPools.add(threadPool);
        }

        onUpdatedSimilarList.run();
    }
}
