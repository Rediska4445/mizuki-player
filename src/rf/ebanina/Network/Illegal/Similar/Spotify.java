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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static rf.ebanina.UI.UI.Paint.ColorProcessor.isPreserveRatio;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isSmooth;

public class Spotify
        implements ISimilar
{
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private final List<Future<?>> currentSimilarTasks = new ArrayList<>();

    public void clearTasks() {
        for (Future<?> f : currentSimilarTasks) {
            if (!f.isDone()) {
                f.cancel(true);
            }
        }

        currentSimilarTasks.clear();
    }

    @Override
    public void updateSimilar(Track track) {
        try {
            me.API.Album.Track[] getSimilar = me.API.Info.info.getSimilarTracks(
                    URLEncoder.encode(track.viewName(), StandardCharsets.UTF_8), 50);

            for (me.API.Album.Track t : getSimilar) {
                Track tr = new Track(Info.PlayersTypes.URI_NULL.getCode())
                        .setNetty(true)
                        .setViewName(t.getAuthor() + " - " + t.getTitle())
                        .setTitle(t.getTitle())
                        .setArtist(t.getAuthor())
                        .setExternalUrl(Info.PlayersTypes.SPOTIFY.getCode())
                        .setMipmap(ResourceManager.Instance.loadImage(Info.PlayersTypes.SPOTIFY.getCode(), 40, 40, isPreserveRatio, isSmooth));

                Music.mainLogger.info(tr.getArtist() + " " + tr.getTitle());

                tr.metadata.put("album_art", t.getAwesomeAlbumArt().getUrl(), String.class);
                tr.metadata.put("mipmap_is_loaded", false, boolean.class);

                Platform.runLater(() -> {
                    Root.rootImpl.similar.getTrackListView().getItems().add(tr);
                    Root.PlaylistHandler.playlistHandler.playlistSimilar.add(tr);
                    PlayProcessor.playProcessor.getTracks().addAll(Root.rootImpl.similar.getTrackListView().getItems());
                });

                Future<?> f = executor.submit(() -> {
                    if (ConfigurationManager.instance.getBooleanItem("delayed_loading", "true") && Root.rootImpl.similar.isVisible()) {
                        Track uri = new Track();

                        try {
                            uri = Track.parseTrackFromNetworkAsync(tr.viewName);
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (uri.getPath() != null) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Track> getSimilar(String f) {
        return List.of();
    }
}
