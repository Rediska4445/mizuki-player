package rf.ebanina.Network.Illegal.Similar;

import javafx.application.Platform;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.Network.ISimilar;
import rf.ebanina.Network.Illegal.ConcurrentSimilar;
import rf.ebanina.Network.Net;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Spotify
        extends ConcurrentSimilar implements ISimilar
{
    @Override
    public void updateSimilar(Track track) {
        try {
            Music.mainLogger.info("Spotify is started");

            me.API.Album.Track[] getSimilar = me.API.Info.info.getSimilarTracks(
                    URLEncoder.encode(track.viewName(), StandardCharsets.UTF_8), 50);

            Music.mainLogger.info("Spotify got the similars: " + Arrays.toString(getSimilar));

            for (me.API.Album.Track t : getSimilar) {
                Track tr = new Track(Net.PlayersTypes.URI_NULL.getCode())
                        .setNetty(true)
                        .setViewName(t.getAuthor() + " - " + t.getTitle())
                        .setTitle(t.getTitle())
                        .setArtist(t.getAuthor())
                        .setAlbumArt(Track.createAlbumArt(t.getAwesomeAlbumArt().getUrl()))
                        .setMipmap(Track.createMipmap(t.getAlbumArts().size() > 0
                                ? t.getAlbumArts().get(0).getUrl()
                                : ResourceManager.Instance.loadResource(Net.PlayersTypes.SPOTIFY.getCode())
                        ));

                tr.getProperties().put("album_art", t.getAwesomeAlbumArt().getUrl(), String.class);
                tr.putProperty(Track.Properties.EXTERNAL_URI, Net.PlayersTypes.SPOTIFY.getName(), String.class);
                tr.putProperty(Track.Properties.MIPMAP_IS_LOADED, false, boolean.class);

                Platform.runLater(() -> {
                    Root.rootImpl.similar.getTrackListView().getItems().add(tr);
                    Root.PlaylistHandler.playlistHandler.playlistSimilar.add(tr);
                    PlayProcessor.playProcessor.getTracks().addAll(Root.rootImpl.similar.getTrackListView().getItems());
                });

                // TODO: Вынести во что то общее
                Future<?> f = executor.submit(() -> {
                    if (ConfigurationManager.instance.getBooleanItem("delayed_loading", "true") && Root.rootImpl.similar.isVisible()) {
                        Track uri = new Track();

                        try {
                            uri = Track.parseTrackFromNetworkAsync(tr.viewName);
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (uri.getPath() != null) {
                            Music.mainLogger.info("Track uri from Spotify: " + uri.getPath());

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

    @Override
    public String toString() {
        return "Spotify{" +
                "executor=" + executor +
                ", currentSimilarTasks=" + currentSimilarTasks +
                '}';
    }
}
