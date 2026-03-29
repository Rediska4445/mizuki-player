package rf.ebanina.Network;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.util.Duration;
import org.json.simple.parser.ParseException;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Metadata.MetadataOfFile;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.ebanina.Player.Media;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.utils.concurrency.LonelyThreadPool;
import rf.ebanina.utils.loggining.Prefix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static rf.ebanina.Network.Info.playersMap;
import static rf.ebanina.UI.Root.soundSlider;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.logo;

@Deprecated(since = "1.4.7-1.1.4")
public class OnlineTrack {
    public interface IParseAlbumArt {
        Image getAlbumArt(String view, int height, int width, boolean isPreserveRatio, boolean isSmooth);
    }

    public static final List<IParseAlbumArt> PARSE_ALBUM_ARTS = new ArrayList<>(List.of(
            (view, height, width, isPreserveRatio, isSmooth) -> {
                String url;

                try {
                    url = me.API.Info.info.search(URLEncoder.encode(view, StandardCharsets.UTF_8)).getAwesomeAlbumArt().getUrl();
                } catch (IOException | ParseException e) {
                    throw new RuntimeException(e);
                }

                if(url != null && !url.isEmpty()) {
                    return new Image(url, height, width, isPreserveRatio, isSmooth);
                }

                return logo;
            }
    ));

    public static Image parseImage(String view, int height, int width, boolean isPreserveRation, boolean isSmooth) {
        try(ExecutorService executor = Executors.newFixedThreadPool(PARSE_ALBUM_ARTS.size())) {
            List<Callable<Image>> tasks = new ArrayList<>();
            for (IParseAlbumArt func : PARSE_ALBUM_ARTS) {
                tasks.add(() -> func.getAlbumArt(view, height, width, isPreserveRation, isSmooth));
            }

            List<Future<Image>> futures = executor.invokeAll(tasks, ConfigurationManager.instance.getIntItem("network_art_parse_timeout", "2"), TimeUnit.SECONDS);

            for (Future<Image> f : futures) {
                if (f.isDone() && !f.isCancelled()) {
                    Image result = f.get();

                    if (result != null) {
                        executor.shutdownNow();
                        return result;
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            return logo;
        }

        return logo;
    }

    public static Image parseImage(String view, int height, int width) {
        return parseImage(view, height, width, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth);
    }

    public static Image parseImage(String view) {
        return parseImage(view, ColorProcessor.size, ColorProcessor.size);
    }

    private static String media;

    //FIXME: Выяснить, почему при возможности быстрого прослушивания, треки автоматически скипаются
    public static Track trackParseAsync(String track) throws IOException {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            try {
                List<Callable<Track>> tasks = new ArrayList<>();

                for (Info.IInfo a : playersMap.values()) {
                    tasks.add(() -> {
                        Track url = a.getTrackDownloadLink(track);

                        if (url == null) {
                            throw new IOException("Invalid URL");
                        }

                        return url;
                    });
                }

                return executor.invokeAny(tasks);
            } catch (InterruptedException | ExecutionException e) {
                Music.mainLogger.warn("Не удалось получить URL для трека: " + track);
                return null;
            } finally {
                executor.shutdownNow();
            }
        }
    }

    public static void downloadAndSaveDataAttributeToDirectory(rf.ebanina.ebanina.Player.Track newValue, java.nio.file.Path directory) {
        String ext = ".mp3";

        URL url = getURIFromTrack(newValue);

        if (url != null) {
            Path outputPath = Path.of(directory + File.separator + newValue.viewName() + ext);

            try (InputStream in = url.openStream()) {
                Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }

            MetadataOfFile.iMetadataOfFiles.setArt(directory + File.separator + newValue.viewName() + ext,
                    SwingFXUtils.fromFXImage(parseImage(newValue.viewName()), null));
        }
    }

    public static URL getURIFromTrack(rf.ebanina.ebanina.Player.Track newValue) {
        try {
            String urlString = newValue.toString();

            if (urlString == null || urlString.equalsIgnoreCase(Info.PlayersTypes.URI_NULL.getCode())) {

                urlString = Objects.requireNonNull(trackParseAsync(newValue.viewName())).getPath();

                if (urlString == null) {
                    Music.mainLogger.println(Prefix.ERROR, "URL для загрузки отсутствует");

                    return null;
                }
            }

            return new URL(urlString);
        } catch (IOException e) {
            Music.mainLogger.err(e);

            return null;
        }
    }

    public static Consumer<rf.ebanina.ebanina.Player.Track> inetPlay = new Consumer<>() {
        @Override
        public synchronized void accept(rf.ebanina.ebanina.Player.Track newValue) {
            try {
                URL res = newValue.getPath() == null || newValue.getPath().equals(Info.PlayersTypes.URI_NULL.getCode())
                        ? getURIFromTrack(newValue) : new URL(newValue.getPath());

                final boolean isPreDownload = ConfigurationManager.instance.getBooleanItem("network_pre_download", "false");

                if (res != null) {
                    if (isPreDownload) {
                        if(FileManager.instance.isOccupiedSpace(Resources.Properties.DEFAULT_INET_CACHE_PATH.getKey(), 200)) {
                            FileManager.instance.clearCacheData(Resources.Properties.DEFAULT_INET_CACHE_PATH.getKey());
                        }

                        media = java.nio.file.Path.of(Resources.Properties.DEFAULT_INET_CACHE_PATH.getKey() + File.separator
                                + newValue.viewName() + ".mp3").toFile().getAbsolutePath();

                        if(!Files.exists(Paths.get(media))) {
                            Files.copy(res.openStream(), Paths.get(media), StandardCopyOption.REPLACE_EXISTING);
                        }

                        soundSlider.loadSliderBackground(new File(media));
                    } else {
                        media = String.valueOf(res);

                        soundSlider.setupDefaultBox();
                    }
                } else {
                    Music.mainLogger.println("Failed to load");

                    media = null;
                }

                Platform.runLater(() -> Root.currentArtist.setText(newValue.getArtist()));
                Platform.runLater(() -> Root.currentTrackName.setText(newValue.getTitle().replace("-", "")));

                Root.artProcessor.initArt(newValue);

                Platform.runLater(() -> {
                    MediaProcessor.mediaProcessor.mediaPlayer.stop();
                    MediaProcessor.mediaProcessor.mediaPlayer.dispose();

                    MediaProcessor.mediaProcessor.setNewMedia(MediaProcessor.mediaProcessor.hit = new Media(media, isPreDownload));

                    if(!isPreDownload) {
                        MediaProcessor.mediaProcessor.mediaPlayer.pause();
                        MediaProcessor.mediaProcessor.mediaPlayer.setTotalOverDuration(Duration.seconds(totalDuraSec = newValue.getTotalDuraSec()));
                    } else {
                        MediaProcessor.mediaProcessor.mediaPlayer.play();
                    }

                    MediaProcessor.mediaProcessor.readState(newValue);

                    soundSlider.setMax(MediaProcessor.mediaProcessor.mediaPlayer.getOverDuration().toSeconds());
                    Root.endTime.setText(rf.ebanina.ebanina.Player.Track.getFormattedTotalDuration((int) soundSlider.getMax()));
                });
            } catch (NullPointerException | IOException e) {
                e.printStackTrace();
            }
        }
    };

    public static int totalDuraSec = 0;

    private static final LonelyThreadPool exec = new LonelyThreadPool();

    public static void play(rf.ebanina.ebanina.Player.Track newValue) {
        exec.runNewTask(() -> inetPlay.accept(newValue));
    }
}