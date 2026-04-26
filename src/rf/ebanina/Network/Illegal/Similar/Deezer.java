package rf.ebanina.Network.Illegal.Similar;

import javafx.application.Platform;
import javafx.scene.image.Image;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.Network.ISimilar;
import rf.ebanina.Network.ITypicalSimilar;
import rf.ebanina.Network.Illegal.ConcurrentSimilar;
import rf.ebanina.Network.Net;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Deezer
        extends ConcurrentSimilar implements ITypicalSimilar<deezer.Deezer>
{
    protected deezer.Deezer deezer = new deezer.Deezer();

    private final Map<String, Integer> deezerSettings = new HashMap<>(Map.ofEntries(
            Map.entry("topTrackByArtistLimit", 6),
            Map.entry("anotherTopTrackOfArtistLimit", 6),
            Map.entry("searchLimit", 6),
            Map.entry("chartLimit", 0),
            Map.entry("depth", 3)
    ));

    public Map<String, Integer> getDeezerSettings() {
        return deezerSettings;
    }

    @Override
    public deezer.Deezer original() {
        return deezer;
    }

    @Override
    public void updateSimilar(Track track) {
        List<Track> tracks = new ArrayList<>();

        try {
            List<deezer.models.Track> list = deezer.getRelatedTracks(
                    track.getViewName(),
                    deezerSettings.get("topTrackByArtistLimit"),
                    deezerSettings.get("anotherTopTrackOfArtistLimit"),
                    deezerSettings.get("searchLimit"),
                    deezerSettings.get("chartLimit"),
                    deezerSettings.get("depth")
            );

            if(list.size() == 0) {
                Music.mainLogger.warn("Deezer не нашёл нихуя");
            }

            for(deezer.models.Track track1 : list) {
                Track tr = new Track()
                        .setTitle(track1.getTitle())
                        .setArtist(track1.getArtist().getName())
                        .setTotalDuraSec(track1.getDuration())
                        .setNetty(true)
                        .setAlbumArt(new Image(track1.getAlbum().getCover_big()))
                        .setMipmap(Track.createMipmap(track1.getAlbum().getCover_small())
                );

                tracks.add(tr);

                tr.getProperties().put(Track.Properties.EXTERNAL_URI.getName(), Net.PlayersTypes.DEEZER.getName(), String.class);
                tr.getProperties().put("album_art", track1.getAlbum().getCover_big(), String.class);
                tr.getProperties().put("mipmap_is_loaded", false, boolean.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(Track tr : tracks) {
            Platform.runLater(() -> {
                Root.rootImpl.similar.getTrackListView().getItems().add(tr);
                Root.PlaylistHandler.playlistHandler.playlistSimilar.add(tr);
                PlayProcessor.playProcessor.getTracks().addAll(Root.rootImpl.similar.getTrackListView().getItems());
            });

            Future<?> f = executor.submit(() -> {
                if (ConfigurationManager.instance.getBooleanItem("delayed_loading", "true") && Root.rootImpl.similar.isVisible()) {
                    Track uri = new Track();

                    try {
                        uri = Track.parseTrackFromNetworkAsync(tr.viewName());
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (uri.getPath() != null) {
                        Music.mainLogger.info("Track uri from Deezer: " + uri.getPath());

                        tr.setPath(uri.getPath());
                        tr.setTotalDuraSec(uri.getTotalDuraSec());
                    } else {
                        Platform.runLater(() -> {
                            Root.rootImpl.similar.getTrackListView().getItems().remove(tr);
                            Root.PlaylistHandler.playlistHandler.playlistSimilar.remove(tr);
                            PlayProcessor.playProcessor.getTracks().remove(tr);
                        });
                    }
                }
            });

            currentSimilarTasks.add(f);
        }
    }

    @Override
    public List<Track> getSimilar(String f) {
        return null;
    }
}
