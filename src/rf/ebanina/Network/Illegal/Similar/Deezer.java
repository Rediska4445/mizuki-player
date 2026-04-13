package rf.ebanina.Network.Illegal.Similar;

import javafx.application.Platform;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.Network.ISimilar;
import rf.ebanina.Network.Info;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static rf.ebanina.UI.UI.Paint.ColorProcessor.isPreserveRatio;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isSmooth;

public class Deezer
        implements ISimilar
{
    protected deezer.Deezer deezer = new deezer.Deezer();

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private final List<Future<?>> currentSimilarTasks = new ArrayList<>();

    @Override
    public void updateSimilar(Track track) {
        List<Track> tracks = new ArrayList<>();

        try {
            for(deezer.models.Track track1 : deezer.getRelatedTracks(
                    track.getViewName(), 3, 1, 5, 0, 3
            )) {
                Track tr = new Track()
                        .setTitle(track1.getTitle())
                        .setArtist(track1.getArtist().getName())
                        .setTotalDuraSec(track1.getDuration())
                        .setNetty(true)
                        .setExternalUrl(Info.PlayersTypes.DEEZER.getCode())
                        .setMipmap(ResourceManager.Instance.loadImage(Info.PlayersTypes.APPLE.getCode(), 40, 40, isPreserveRatio, isSmooth)
                );

                tracks.add(tr);

                tr.metadata.put("mipmap_is_loaded", false, boolean.class);
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
