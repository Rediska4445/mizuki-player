package rf.ebanina.Network;

import javafx.application.Platform;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.Network.Illegal.Download.Hitmos;
import rf.ebanina.Network.Illegal.Download.LightAudio;
import rf.ebanina.Network.Illegal.Download.MusMore;
import rf.ebanina.Network.Illegal.Similar.Apple;
import rf.ebanina.Network.Illegal.Similar.LastFM;
import rf.ebanina.Network.Illegal.Similar.SoundCloud;
import rf.ebanina.Network.Illegal.Similar.Spotify;

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

    private static final ExecutorService exec = Executors.newSingleThreadExecutor();

    private static volatile boolean isSimilarUpdateState = true;

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
