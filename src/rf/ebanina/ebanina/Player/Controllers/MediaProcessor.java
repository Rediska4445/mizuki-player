package rf.ebanina.ebanina.Player.Controllers;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Duration;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Field;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.File.Metadata.MetadataOfFile;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.Network.Info;
import rf.ebanina.Network.OnlineTrack;
import rf.ebanina.Network.Translator;
import rf.ebanina.UI.Editors.Player.AudioHost;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Element.Slider.SoundSlider;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.UI.UI.Popup.PreviewPopupService;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.AudioDecoder;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlaylistController;
import rf.ebanina.ebanina.Player.Media;
import rf.ebanina.ebanina.Player.MediaPlayer;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.utils.collections.TypicalMapWrapper;
import rf.ebanina.utils.concurrency.LonelyThreadPool;
import rf.ebanina.utils.loggining.Prefix;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static rf.ebanina.File.Field.fields;
import static rf.ebanina.Network.Info.playersMap;
import static rf.ebanina.UI.Root.endTime;
import static rf.ebanina.UI.Root.soundSlider;
import static rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor.playProcessor;
import static rf.ebanina.ebanina.Player.Controllers.Playlist.PlaylistController.checkIndexOutOfBoundPlaylist;

public class MediaProcessor {
    public static MediaProcessor mediaProcessor = new MediaProcessor(
            ConfigurationManager.instance.getBooleanItem("clear_samples", "true"),
            soundSlider
    );

    public MediaProcessor(boolean isClearSamples, SoundSlider soundSliderPointer) {
        this.isClearSamples = isClearSamples;
        this.soundSliderPointer = soundSliderPointer;
    }

    /**
     * Карта параметров медиаплеера, хранящая свойства JavaFX для управления состоянием плеера.
     * <p>
     * Содержит основные флаги воспроизведения:
     * </p>
     * <ul>
     *     <li>{@code "isAutoPlayback"} - автоматическое воспроизведение следующего трека</li>
     *     <li>{@code "isAutoPlaylist"} - автоматический переход к следующему плейлисту</li>
     *     <li>{@code "isPlayRandom"} - случайное воспроизведение треков</li>
     * </ul>
     * <p>
     * Пример использования:
     * </p>
     * <pre>
     * {@code
     * // Получить свойство
     * BooleanProperty autoPlayback = (BooleanProperty) mediaParameters.get("isAutoPlayback");
     *
     * // Установить значение
     * autoPlayback.set(true);
     *
     * // Добавить слушатель изменений
     * autoPlayback.addListener((obs, old, newVal) -> {
     *     System.out.println("Auto playback: " + newVal);
     * });
     *
     * // Проверить текущее состояние
     * boolean isRandom = (BooleanProperty) mediaParameters.get("isPlayRandom")
     *         .getValue();
     * }
     * </pre>
     */
    public HashMap<String, Property<?>> mediaParameters = new HashMap<>(Map.ofEntries(
            Map.entry("isAutoPlayback", new SimpleBooleanProperty(false)),
            Map.entry("isPlaylistLoop", new SimpleBooleanProperty(true)),
            Map.entry("isPlayRandom", new SimpleBooleanProperty(false))
    ));

    public enum MediaParameters {
        IS_AUTO_PLAYBACK("isAutoPlayback"),
        IS_PLAYLIST_LOOP("isPlaylistLoop"),
        IS_PLAY_RANDOM("isPlayRandom");

        public final String code;

        MediaParameters(String code) {
            this.code = code;
        }
    }

    public final int MEDIA_PLAYER_BLOCK_SIZE_FRAMES = ConfigurationManager.instance.getIntItem("audio_player_block_size_frames", "256");

    private final LonelyThreadPool _trackSingleAloneThread = new LonelyThreadPool();

    private final AtomicInteger trackCacheIter = new AtomicInteger(0);

    // Исполнитель для скипов дропов, интро, и прочей хуйни
    private final ExecutorService skipExec = Executors.newSingleThreadExecutor();

    // Плеер - СВОЙ, НЕ JAVAFX!
    public MediaPlayer mediaPlayer;

    // Типовая карта для обмена общими для плеера данными (типа громкости, пана)
    public TypicalMapWrapper<String> globalMap = new TypicalMapWrapper<>();

    // Текущий медиа-ресурс
    public rf.ebanina.ebanina.Player.Media hit;

    // Список декодеров, которые используются для работы с другими формата (не mp3, wav).
    // AudioDecoder создан извне - НО - его нужно обновить (методы, в интерфейсе)!
    public final List<AudioDecoder> decoderList = new ArrayList<>();

    // Автоматизированная инициализация настроек для плеера.
    public void initializeAudioParameters(MediaPlayer mediaPlayer) {
        for(PluginWrapper pluginWrapper : AudioHost.instance.vstPlugins) {
            pluginWrapper.turnOn();
        }

        mediaPlayer
                // Плагины (Steinberg, Au, Ivl2 и типо того)
                .setPlugins(AudioHost.instance.vstPlugins)
                // Громкость
                .setVolume(globalMap.get("volume", double.class))
                // Темп
                .setTempo((globalMap.get("tempo", float.class)))
                // Панорамирование
                .setPan(globalMap.get("pan", float.class))
                // Декодеры
                .setDecoders(decoderList);

        // Аудио выход, указывается как название
        if(String.valueOf(globalMap.get("audio_out", String.class)) != null
                && !String.valueOf(globalMap.get("audio_out", String.class)).equals("")) {
            try {
                mediaPlayer.setAudioOutput(String.valueOf(globalMap.get("audio_out", String.class)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Дефолтный переключатель.
    // Просто вызвав его, плеер будет автоматически переключать трек на текущий указатель trackIter в PlayProcessor.
    // Запускает новую одинокую-задачу (LonelyThreadPool).
    // В сетевом проигрывании бывает дурка, когда плеер не останавливает предыдущий MediaPlayer поток.
    // Это необъяснимо, ибо при переключении хуй знает сколько раз вызывается mediaPlayer.stop();, что по логике останавливает плеер и его потоки, независимо от сторонних потоков.
    // Единственное, что может быть - поток "Одинокий", он закрывается и не успевает закрыть предыдущие потоки - но это хуйня, ибо потоки плеера закрываются при запуске нового одинокого-потока
    public void _track() {
        _trackSingleAloneThread.runNewTask(() -> {
            // Проверка на выход за границы
            if(playProcessor.getTrackIter() >= playProcessor.getTracks().size()) {
                if (mediaParameters.get(MediaParameters.IS_PLAYLIST_LOOP.code).getValue().equals(true)) {
                    playProcessor.setTrackIter(0);
                } else {
                    PlaylistController.playlistController.next();
                }
            } else if(playProcessor.getTrackIter() < 0) {
                if(mediaParameters.get(MediaParameters.IS_PLAYLIST_LOOP.code).getValue().equals(true)) {
                    playProcessor.setTrackIter(PlayProcessor.playProcessor.getTracks().size() - 1);
                } else {
                    PlaylistController.playlistController.down();
                }
            }

            // Проверка на выход за пределы плейлиста
            checkIndexOutOfBoundPlaylist();

            _track(playProcessor.getTracks().get(playProcessor.getTrackIter()));
        }, () -> {
//            mediaPlayer.stop();
//            mediaPlayer.close();
//            mediaPlayer.dispose();
        });
    }

    //TODO: Обязательно слить сетевое воспроизведение с локальным!
    //TODO: Так, получается приходится дублировать код!
    //TODO: При слиянии, и отдельной логике внутри методов через Track.isNetty(), можно добиться универсальности!
    public void _track(Track track) {
        // Задержка
        int delay = ConfigurationManager.instance.getIntItem("delay_between_play", "2000");

        if(delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Включение
        if (mediaPlayer != null) {

            // Фокус ячейки
            Platform.runLater(() -> {
                if(track.isNetty()) {
                    Root.similar.getTrackListView().getSelectionModel().select(playProcessor.getTrackIter());
                } else {
                    Root.tracksListView.getTrackListView().getSelectionModel().select(playProcessor.getTrackIter());
                }
            });

            // Сам плеер
            prepareToPlay(track);

            // Графическая хуйня (обложка, название, автор и т.д.)
            updateInfo(track);
        }
    }

    public void updateInfo(Track track) {
        Root.artProcessor.initArt(track);

        if(track.isNetty()) {
            Platform.runLater(() -> {
                Root.currentArtist.setText(track.getArtist());
                Root.currentTrackName.setText(track.getTitle().replace("-", ""));

                soundSlider.setMax(mediaPlayer.getOverDuration().toSeconds());
                Root.endTime.setText(rf.ebanina.ebanina.Player.Track.getFormattedTotalDuration((int) soundSlider.getMax()));

                if(track.metadata.get("netty_file_path", String.class).equalsIgnoreCase("null")) {
                    soundSlider.setupDefaultBox();
                } else {
                    Root.rootImpl.loadRectangleOfGainVolumeSlider(new File(track.metadata.get("netty_file_path", String.class)));
                }
            });
        } else {
            Root.rootImpl.loadRectangleOfGainVolumeSlider(new File(track.getPath()));

            String endTime = track.getFormattedTotalDuration();
            String author = new String(track.getArtist().getBytes(), StandardCharsets.UTF_8);
            String title = new String(track.getTitle().getBytes(), StandardCharsets.UTF_8);
            String startTime = Track.getFormattedTotalDuration((int) mediaPlayer.getCurrentTime().toSeconds());

            playProcessor.getTracks().get(playProcessor.getTrackIter()).setArtist(author);
            playProcessor.getTracks().get(playProcessor.getTrackIter()).setTitle(title);

            Platform.runLater(() -> {
                Root.endTime.setText(endTime);
                Root.beginTime.setText(startTime);
                Root.currentArtist.setText(author);
                Root.currentTrackName.setText(title);
            });
        }

        PreviewPopupService.updateAll();

        if(trackCacheIter.get() > Track.CACHE_SIZE) {
            for (Track track_ : playProcessor.getTracks()) {
                track_.setAlbumArt(null);
                track_.setMipmap(null);
            }

            trackCacheIter.set(0);
        }

        trackCacheIter.incrementAndGet();

        if(ConfigurationManager.instance.getBooleanItem("translate_track_title", "false")) {
            Root.currentTrackName.setText(Translator.instance.TranslateNodeText(
                    playProcessor.getTracks().get(playProcessor.getTrackIter()).getTitle(),
                    LocalizationManager.instance.lang.substring(LocalizationManager.instance.lang.indexOf("_") + 1)
            ));
        }
    }

    public Track trackParseAsync(String track) throws IOException {
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

    public URL getURIFromTrack(rf.ebanina.ebanina.Player.Track newValue) {
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

    final boolean isPreDownload = ConfigurationManager.instance.getBooleanItem("network_pre_download", "false");

    public void regenerateMediaPlayer(Track track) {
        // Запихать состояние плеера (пауза/плей)
        globalMap.put("pause",
                (mediaPlayer.getStatus() == rf.ebanina.ebanina.Player.MediaPlayer.Status.PAUSED
                        || mediaPlayer.getStatus() == rf.ebanina.ebanina.Player.MediaPlayer.Status.READY),
                boolean.class);

        // Ссылка
        String media = null;

        // Сетевой
        if(track.isNetty()) {
            try {
                // Если у трека нет пути, то получить через сетевые сервисы
                URL res = track.getPath() == null || track.getPath().equals(Info.PlayersTypes.URI_NULL.getCode())
                        ? getURIFromTrack(track) : new URL(track.getPath());

                // Может не получить ссылку
                if (res != null) {

                    // Скачивать перед проигрыванием.
                    // Бля, оно нормально будет работать только при true, ибо даже с JavaFX MediaPlayer без скачивания работал криво.
                    // Много нюансов, которых заёб полный учитывать (не всегда получается длительность, разные форматы потоков, разные типы форматов, разные заголовки, и прочая хуйня)
                    if (isPreDownload) {

                        // Очистить папку с треками, если больше чем 64 чего то
                        if(FileManager.instance.isOccupiedSpace(Resources.Properties.DEFAULT_INET_CACHE_PATH.getKey(), 64)) {

                            // Чистка
                            FileManager.instance.clearCacheData(Resources.Properties.DEFAULT_INET_CACHE_PATH.getKey());
                        }

                        // Подготовить место для файла трека
                        media = java.nio.file.Path.of(Resources.Properties.DEFAULT_INET_CACHE_PATH.getKey(), track.viewName() + ".mp3").toFile().getAbsolutePath();

                        // Удобно
                        final Path path = Paths.get(media);

                        // Если файл уже скачан, нехуй его скачивать ещё раз.
                        if(!Files.exists(path)) {

                            // Скачивание с сетевого потока на локальный путь
                            Files.copy(res.openStream(), path, StandardCopyOption.REPLACE_EXISTING);
                        }

                        // Путь к локальному файлу
                        track.metadata.put("netty_file_path", media, String.class);
                    } else {
                        // Может, и получится слушать как на стриминговых сервисах, но вряд-ли.
                        // Господь пастырь мой...
                        media = String.valueOf(res);

                        // Нужно для генерации стандартной SoundSlider.
                        // В действительности, нужен весь поток для обработки, поэтому приходится скачивать.
                        track.metadata.put("netty_file_path", "null", String.class);
                    }
                } else {
                    Music.mainLogger.warn("Failed to load");

                    // Такого трека не найдено

                    media = null;
                }

                // Остановка и пауза на новом треке.
                // Это конечно пиздец, но поток может не остановится.
                // Поток также проскипает байты, если не поставить на паузу
                MediaProcessor.mediaProcessor.mediaPlayer.stop();
                MediaProcessor.mediaProcessor.mediaPlayer.dispose();
                MediaProcessor.mediaProcessor.mediaPlayer.pause();

                // Генерация
                setNewMedia(hit = new Media(media, track.isNetty()));

                // Длительность из потока не вычислить.
                // Берётся длительность из самого трека, которая должна быть заложена при парсинге.
                // Может быть прокатит, и получится проиграть на не preDownload
                if(!isPreDownload) {
                    MediaProcessor.mediaProcessor.mediaPlayer.setTotalOverDuration(Duration.seconds(track.getTotalDuraSec()));
                    totalDuraSec.set(track.getTotalDuraSec());
                }
            } catch (NullPointerException | IOException e) {
                e.printStackTrace();
            }
        } else {

            // Не сетевой
            mediaPlayer.stop();
            mediaPlayer.dispose();

            media = new File(track.getPath()).toURI().toString();
        }

        if(media == null) {
            throw new RuntimeException("Media is null");
        }

        // Генерация
        setNewMedia(hit = new Media(media, track.isNetty()));
    }

    // Нужно для не preDownload состояния
    public SimpleIntegerProperty totalDuraSec = new SimpleIntegerProperty(0);

    public void prepareToPlay(Track track) {
        // Генерация нового плеера
        regenerateMediaPlayer(track);

        // Кэш (время, темп, прочая хуйня)
        readState(track);

        // Если в текущее проигрывание - пауза, то не включать
        if (!globalMap.get("pause", boolean.class)) {

            // Анимация громкости вручную
            fadePlay(0, 1, true);
        }
    }

    public void fadePlay(float startVolume, float endVolume, boolean intro) {
        final int durationMs = 500;
        final int framesPerSecond = 60;
        final int frameTimeMs = 1000 / framesPerSecond;
        final int totalFrames = durationMs / frameTimeMs;

        mediaPlayer.setLineVolume(startVolume);

        if(intro && mediaPlayer.getMedia().getIntroSoundFile().exists()) {
            mediaPlayer.playIntro();
            mediaPlayer.play();
        } else {
            mediaPlayer.play();
        }

        new Thread(() -> {
            for (int frame = 0; frame <= totalFrames; frame++) {
                float t = (float) frame / totalFrames;
                float volume = startVolume + t * (endVolume - startVolume);
                mediaPlayer.setVolume(volume);

                try {
                    Thread.sleep(frameTimeMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            mediaPlayer.setVolume(endVolume);
        }).start();
    }

    public void pause_play() {
        if(mediaPlayer.getStatus().equals(rf.ebanina.ebanina.Player.MediaPlayer.Status.PAUSED)
                || mediaPlayer.getStatus().equals((rf.ebanina.ebanina.Player.MediaPlayer.Status.READY))) {
            fadePlay(0, 1, false);
        } else {
            pause();
        }
    }

    public void pause() {
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void setTempo(float tempo) {
        if (mediaPlayer != null) {
            mediaPlayer.setTempo(tempo);
            globalMap.put("tempo", tempo, float.class);

            double durationSeconds = mediaPlayer.recalculateOverDuration().toSeconds();

            Platform.runLater(() -> {
                Root.soundSlider.setMax(durationSeconds);
                endTime.setText(Track.getFormattedTotalDuration((float) soundSlider.getMax()));
            });

            ColorProcessor.core.scaleHue(tempo);
        }
    }

    public void initGlobalMap() {
        globalMap.put("volume", Double.parseDouble(FileManager.instance.readSharedData().get("volume")), double.class);
        globalMap.put("pitch", Float.parseFloat(FileManager.instance.readSharedData().get("pitch")), float.class);
        globalMap.put("pan", Float.parseFloat(FileManager.instance.readSharedData().get("pan")), float.class);
        globalMap.put("pause", Boolean.parseBoolean(FileManager.instance.readSharedData().get("pause")), boolean.class);
        globalMap.put("tempo", Float.parseFloat(FileManager.instance.readSharedData().get("tempo")), float.class);
        globalMap.put("audio_out", ConfigurationManager.instance.getItem("audio_out", ""), String.class);
    }

    // Стартовое состояние
    public void updateMediaPlayer() {
        double sec = Double.parseDouble(FileManager.instance.readSharedData().get("last_track_time"));

        mediaProcessor._track();

        if (globalMap.get("pause", boolean.class)) {
            mediaPlayer.setOnReady(() -> mediaPlayer.pause());
        } else {
            mediaPlayer.setOnReady(() -> mediaPlayer.play());
        }

        Platform.runLater(() -> {
            soundSlider.setValue(sec);
            soundSlider.setMax(MetadataOfFile.iMetadataOfFiles.getDuration(playProcessor.getTracks().get(playProcessor.getTrackIter()).toString()));

            Root.trackSelectionModel.select(playProcessor.getTrackIter());
            Root.beginTime.setText((Track.getFormattedTotalDuration(sec)));
        });
    }

    // Чтение или запись данных о треке
    public void readState(Track track) {
        if (PlayProcessor.playProcessor.getTrackHistoryGlobal().size() >= ConfigurationManager.instance.getIntItem("global_history_size", "25")) {
            PlayProcessor.playProcessor.getTrackHistoryGlobal().getHistory().clear();
        }

        Path path;

        String trackInFile = track.getPath();

        final String al = ConfigurationManager.instance.getItem("start_play_from", "last_time");

        // Путь к кеш-файлу
        if(playProcessor.isNetwork()) {
            path = Path.of(
                    Resources.Properties.DEFAULT_INET_TRACKS_CACHE_PATH.getKey()
            ).toAbsolutePath();

            trackInFile = track.getViewName();
        } else {
            path = Path.of(
                    Resources.Properties.DEFAULT_CACHE_TRACKS_PATH.getKey(),
                    FileManager.instance.name(playProcessor.getCurrentPlaylist().get(playProcessor.getCurrentPlaylistIter()).getName())
            ).toAbsolutePath();
        }

        Map<String, String> trackArray = new HashMap<>();

        if (track.isRestoreState()) {
            Map<String, String> state = FileManager.instance.readArray(
                    path.toString(),
                    trackInFile,
                    Map.of()
            );

            if (!state.isEmpty()) {
                // Заготовка действий, на случай если есть кеш в файле
                Map<String, Consumer<String>> stateItems = new HashMap<>(Map.ofEntries(
                        Map.entry(Field.DataTypes.TEMPO.code, (val) -> {
                            globalMap.put("tempo", Float.parseFloat(val), float.class);
                            mediaPlayer.setTempo(Float.parseFloat(val));
                        }),
                        Map.entry(Field.DataTypes.VOLUME.code, (val) -> {
                            globalMap.put("volume", Double.parseDouble(val), double.class);
                            mediaPlayer.setVolume(Double.parseDouble(val));
                        }),
                        Map.entry(Field.DataTypes.PAN.code, (val) -> {
                            globalMap.put("pan", Float.parseFloat(val), float.class);
                            mediaPlayer.setPan(Float.parseFloat(val));
                        }),
                        Map.entry(Field.DataTypes.PITCH.code, (val) -> {
                            globalMap.put("pitch", Float.parseFloat(val), float.class);
                            mediaPlayer.setPan(Float.parseFloat(val));
                        })
                ));

                // Чтение кеша
                trackArray = FileManager.instance.readArray(
                        path.toAbsolutePath().toString(),
                        trackInFile,
                        Map.of()
                );

                for (Map.Entry<String, Consumer<String>> item : stateItems.entrySet()) {
                    if (state.get(item.getKey()) != null) {
                        String val = trackArray.get(item.getKey());

                        if (val != null) {
                            item.getValue().accept(val);
                        }
                    }
                }
            }
        }

        switch (al) {
            case "last_time" -> {
                if (track.isRestoreState() && PlayProcessor.playProcessor.getTrackHistoryGlobal().contains(track)) {
                    Duration dura = Duration.seconds(Double.parseDouble(trackArray.getOrDefault(Field.DataTypes.TIME.code, "0")));

                    if (dura.toSeconds() < mediaPlayer.getOverDuration().toSeconds() - 30 && dura.toSeconds() > -1) {
                        mediaPlayer.setStartTime(dura);
                    }
                }
            }
            case "skip_intro" -> soundSlider.setOnLoadedSliderBackground(() -> setCurrentTime(Duration.seconds(getSkipIntroPoint(track.getPath()))));
            case "skip_pit" -> soundSlider.setOnLoadedSliderBackground(() -> setCurrentTime(Duration.seconds(getSkipPitPoint(track.getPath(), mediaPlayer.getCurrentTime().toSeconds(), mediaPlayer.getOverDuration().toSeconds()))));
            case "skip_drop" -> soundSlider.setOnLoadedSliderBackground(() -> setCurrentTime(Duration.seconds(getSkipDropPoint(track.getPath(), mediaPlayer.getCurrentTime().toSeconds(), mediaPlayer.getOverDuration().toSeconds()))));
            case "like_moment" -> {
                Duration dura = Duration.seconds(Double.parseDouble(
                        FileManager.instance.read(path.toString(), playProcessor.getTracks().get(playProcessor.getTrackIter()).toString(), "like_moment_start", String.valueOf(soundSlider.getValue()))
                ));

                mediaPlayer.setStartTime(dura);
            }
        }

        if ((boolean) MediaProcessor.mediaProcessor.mediaParameters.get(MediaProcessor.MediaParameters.IS_AUTO_PLAYBACK.code).getValue()) {
            mediaPlayer.setStartTime(Duration.seconds(Double.parseDouble(
                    FileManager.instance.read(
                            path.toString(),
                            playProcessor.getTracks().get(playProcessor.getTrackIter()).getPath(),
                            fields.get(Field.DataTypes.LIKE_MOMENT_START.code).getLocalName(),
                            "0"))
            ));

            mediaPlayer.setStopTime(Duration.seconds(Double.parseDouble(
                    FileManager.instance.read(
                            path.toString(),
                            playProcessor.getTracks().get(playProcessor.getTrackIter()).getPath(),
                            fields.get(Field.DataTypes.LIKE_MOMENT_START.code).getLocalName(),
                            mediaPlayer.getOverDuration().toString()))
            ));
        }
    }

    public void setCurrentTime(Duration a) {
        mediaPlayer.stop();
        mediaPlayer.close();

        setNewMedia(hit);

        mediaPlayer.setPlugins(AudioHost.instance.vstPlugins);

        if(playProcessor.isNetwork() && !ConfigurationManager.instance.getBooleanItem("network_pre_download", "false")) {
            mediaPlayer.setTotalOverDuration(Duration.seconds(OnlineTrack.totalDuraSec));
        }

        mediaPlayer.setStartTime(a);
        mediaPlayer.play();
    }

    public void setNewMedia(Media media) {
        onMediaPlayerCreate.accept(mediaPlayer = new MediaPlayer(hit = media, MEDIA_PLAYER_BLOCK_SIZE_FRAMES));
    }

    public void mediaPlayerPrepare(MediaPlayer mediaPlayer) {
        initializeAudioParameters(mediaPlayer);

        double currentTimeSeconds = mediaPlayer.getCurrentTime().toSeconds();
        double durationSeconds = mediaPlayer.getOverDuration().toSeconds();

        Platform.runLater(() -> {
            mediaPlayer.setOnPlaying(() -> {
                if (globalMap.get("volume", double.class) <= 0 || globalMap.get("volume", double.class) >= 1.0) {
                    globalMap.put("volume", 1.0, double.class);
                }

                Platform.runLater(() -> {
                    Root.soundSlider.setValue(currentTimeSeconds);
                    Root.soundSlider.setMax(durationSeconds);
                });
            });

            mediaPlayer.setOnStopTimeReached(() -> {
                if (MediaProcessor.mediaProcessor.mediaParameters.get(MediaParameters.IS_AUTO_PLAYBACK.code).getValue().equals(false)) {
                    prepareToPlay(PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()));
                }
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                if (MediaProcessor.mediaProcessor.mediaParameters.get(MediaParameters.IS_PLAY_RANDOM.code).getValue().equals(false)) {
                    if (PlayProcessor.playProcessor.getTrackIter() >= PlayProcessor.playProcessor.getTracks().size()) {
                        if (mediaParameters.get(MediaParameters.IS_PLAYLIST_LOOP.code).getValue().equals(true)) {
                            PlayProcessor.playProcessor.setTrackIter(0);
                        } else {
                            PlaylistController.playlistController.next();
                        }
                    }
                } else {
                    PlayProcessor.playProcessor.setTrackIter(new Random().nextInt(0, PlayProcessor.playProcessor.getTracks().size() - 1));
                }

                if (mediaParameters.get(MediaParameters.IS_AUTO_PLAYBACK.code).getValue().equals(false)) {
                    PlayProcessor.playProcessor.next();
                }
            });
        });
    }

    public Consumer<MediaPlayer> onMediaPlayerCreate = this::mediaPlayerPrepare;

    private final boolean isClearSamples;

    /**
     * Формирует упрощённое представление аудиосигнала для визуализации.
     * <p>
     * Метод считывает все сэмплы из указанного аудиофайла и агрегирует их
     * в укрупнённые временные подмножества (окна). Каждое значение выходного
     * массива соответствует среднему модулю амплитуды в одном таком окне, что
     * позволяет компактно отобразить форму волны трека (waveform) в компоненте
     * {@link SoundSlider}.
     * </p>
     *
     * <h3>Принцип работы</h3>
     * <ul>
     *   <li>Получает исходный массив сэмплов из {@link SoundSlider#getSamples(java.io.File)}.</li>
     *   <li>Определяет количество подмножеств на основе длины массива и частоты кадров
     *       медиаплеера ({@link MediaPlayer#getFrameRate()}) — примерно одно значение на кадр.</li>
     *   <li>Делит весь массив сэмплов на равные по длине сегменты и для каждого
     *       вычисляет среднее значение модуля амплитуды (среднее по |sample|).</li>
     *   <li>Находит максимальное значение среди средних амплитуд и нормализует
     *       весь массив так, чтобы этот максимум соответствовал уровню {@code 32768.0f}.</li>
     *   <li>Пересчитывает нормализованные значения в пиксели относительно текущего
     *       размера компонента {@link SoundSlider#getSize()} — используется половина высоты
     *       компонента как максимальная видимая амплитуда.</li>
     *   <li>При установленном флаге {@code isClearSamples} очищает исходные сэмплы
     *       через {@link SoundSlider#clearSamples()}.</li>
     * </ul>
     *
     * <h3>Назначение</h3>
     * <p>
     * Возвращаемый массив предназначен для графической отрисовки волновой формы
     * аудиодорожки (например, в виде последовательности вертикальных полос или линий),
     * синхронизированной с воспроизведением {@link MediaPlayer}.
     * </p>
     *
     * <h3>Особенности и ограничения</h3>
     * <ul>
     *   <li>Используется усреднение по модулю амплитуды, а не RMS, поэтому значения
     *       отражают субъективную «громкость» сегмента, но не энергию сигнала.</li>
     *   <li>Масштабирование привязано к текущей высоте компонента {@link SoundSlider};
     *       при изменении размера компонента массив рекомендуется пересчитать.</li>
     *   <li>Метод несколько раз обращается к {@link SoundSlider#getSamples(java.io.File)}
     *       внутри циклов; при необходимости оптимизации стоит кэшировать массив сэмплов
     *       в локальную переменную перед циклами.</li>
     * </ul>
     *
     * @param path        путь к аудиофайлу, для которого формируется массив амплитуд.
     * @param soundSlider компонент, предоставляющий доступ к сэмплам и размерам области отрисовки.
     * @param mediaPlayer медиаплеер, задающий частоту кадров для временного разбиения сигнала.
     * @return массив нормализованных и масштабированных средних амплитуд по подмножествам,
     *         готовый для использования при отрисовке волновой формы.
     * @since 0.1.4.x
     */
    public float[] getSubsets(String path, SoundSlider soundSlider, MediaPlayer mediaPlayer) {
        // 1. Загружаем файл по пути
        File file = new File(path);

        // 2. Считываем все сэмплы из файла и определяем:
        //    - сколько временных окон нужно (кол-во сэмплов / FPS плеера)
        //    - сколько сэмплов в каждом окне (общее кол-во / кол-во окон)
        int numSubsets = (int) (soundSlider.getSamples(file).length / mediaPlayer.getFrameRate());
        int subsetLength = soundSlider.getSamples(file).length / numSubsets;

        // 3. Создаём итоговый массив под все окна
        float[] subsets = new float[numSubsets];

        int s = 0;
        for (int i = 0; i < subsets.length; i++) {
            double sum = 0;

            // 4. Для каждого окна суммируем модули всех сэмплов в нём
            for (int k = 0; k < subsetLength; k++) {
                sum += Math.abs(soundSlider.getSamples(file)[s++]);
            }

            // 5. Среднее значение модуля = "громкость" этого окна
            subsets[i] = (float) (sum / subsetLength);
        }

        // 6. Ищем максимальную "громкость" среди всех окон (для нормализации)
        float normal = 0;
        for (float sample : subsets) {
            if (sample > normal)
                normal = sample;
        }

        // 7. Коэффициент нормализации: самый громкий = 32768 (макс. амплитуда PCM)
        normal = 32768.0f / normal;

        // 8. Нормализуем все окна + масштабируем под высоту слайдера
        //    (32768 = макс. амплитуда → height/2 пикселей)
        for (int i = 0; i < subsets.length; i++) {
            subsets[i] *= normal;
            subsets[i] = (subsets[i] / 32768.0f) * (soundSlider.getSize().height / 2);
        }

        // 9. Опционально: очищаем кэш сэмплов (чтобы не жрать память)
        if(isClearSamples) {
            soundSlider.clearSamples();
        }

        // 10. Возвращаем готовые высоты волны в пикселях для отрисовки/skip-intro
        return subsets;
    }

    public SoundSlider soundSliderPointer;

    public int getSkipDropPoint(String path, double currentTime, double duration) {
        final float[] subsets = getSubsets(path, soundSliderPointer, mediaPlayer);

        int n = subsets.length;

        int currentIndex = (int) ((currentTime / duration) * n);
        // Чтобы брать 2 блока слева и справа, нужен запас индексов
        if (currentIndex < 2 || currentIndex > n - 3) {
            return 0; // Недостаточно данных для проверки паттерна
        }

        for (int i = currentIndex; i < n - 2; i++) {
            // Предыдущие два блока
            double prevDiff = Math.abs(subsets[i - 1] - subsets[i - 2]);
            double prevDiff2 = Math.abs(subsets[i] - subsets[i - 1]);

            // Следующие два блока
            double nextDiff = Math.abs(subsets[i + 1] - subsets[i + 2]);
            double nextDiff2 = Math.abs(subsets[i + 2] - subsets[i + 1]);

            // Похожесть предыдущих блоков: малая разница 1-3
            boolean prevSimilar = (prevDiff >= 1 && prevDiff <= 3) && (prevDiff2 >= 1 && prevDiff2 <= 3);
            // Похожесть следующих блоков: малая разница 1-3
            boolean nextSimilar = (nextDiff >= 1 && nextDiff <= 3) && (nextDiff2 >= 1 && nextDiff2 <= 3);

            // Разница между предыдущими и следующими группами: большая разница 5-10
            double betweenGroupDiff1 = Math.abs(subsets[i - 1] - subsets[i + 1]);
            double betweenGroupDiff2 = Math.abs(subsets[i - 2] - subsets[i + 2]);
            boolean bigDiff = (betweenGroupDiff1 >= 5 && betweenGroupDiff1 <= 10)
                    && (betweenGroupDiff2 >= 5 && betweenGroupDiff2 <= 10);

            if (prevSimilar && nextSimilar && bigDiff) {
                // Возвращаем позицию (во времени) обнаруженной ямы
                return (int) ((i / (double) n) * duration);
            }
        }

        return 0;
    }

    public int getSkipIntroPoint(String path) {
        final float[] subsets = getSubsets(path, soundSlider, mediaPlayer);

        for (int i = 0; i < subsets.length - 1; i++) {
            int next_sample = (int) subsets[i + 1];

            int posY = (soundSlider.getSize().height / 2) - next_sample;
            int negY = (soundSlider.getSize().height / 2) + next_sample;

            if((negY - posY + 2) >= 20) {
                return i;
            }
        }

        return 0;
    }

    public int getSkipPitPoint(String path, double currentTime, double duration) {
        final float[] subsets = getSubsets(path, soundSlider, mediaPlayer);

        int n = subsets.length;

        int currentIndex = (int) ((currentTime / duration) * n);
        if (currentIndex >= n - 1) {
            return 0;
        }

        List<Double> diffs = new ArrayList<>();
        for (int i = currentIndex; i < n - 1; i++) {
            double diff = Math.abs(subsets[i + 1] - subsets[i]);
            diffs.add(diff);
        }

        double avgDiff = diffs.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        int pitEndIndex = -1;
        for (int i = 0; i < diffs.size(); i++) {
            if (diffs.get(i) > (avgDiff * 2) + 1) {
                pitEndIndex = currentIndex + i + 1;
                break;
            }
        }

        if (pitEndIndex != -1) {
            double newTime = (pitEndIndex / (double) n) * duration;

            return (int) newTime;
        }

        return 0;
    }

    public int getSkipIntroPoint(String path, int height) {
        final float[] subsets = getSubsets(path, soundSlider, mediaPlayer);

        for (int i = 0; i < subsets.length - 1; i++) {
            int next_sample = (int) subsets[i + 1];

            int posY = (height / 2) - next_sample;
            int negY = (height / 2) + next_sample;

            if((negY - posY + 2) >= 20) {
                return i;
            }
        }

        return 0;
    }

    public void skipIntro(String path) {
        if(mediaPlayer.getCurrentTime().toSeconds() > mediaPlayer.getOverDuration().toSeconds() - 10) {
            playProcessor.next();
        }

        skipExec.submit(() -> setCurrentTime(Duration.seconds(getSkipIntroPoint(path, soundSlider.getSize().height))));
    }

    public void skipPit(String path) {
        skipExec.submit(() -> {
            setCurrentTime(Duration.seconds(getSkipPitPoint(path, mediaPlayer.getCurrentTime().toSeconds(), mediaPlayer.getOverDuration().toSeconds())));
        });
    }

    public void skipOutro(String path) {
        skipExec.submit(() -> {
            final float[] subsets = getSubsets(path, soundSlider, mediaPlayer);

            if(mediaPlayer.getCurrentTime().toSeconds() > (mediaPlayer.getStopTime().toSeconds() - 20)) {
                for (int i = subsets.length - 20; i < subsets.length; i++) {
                    int sample = (int) subsets[i];

                    int posY = (soundSlider.getSize().height / 2) - sample;
                    int negY = (soundSlider.getSize().height / 2) + sample;

                    if ((negY - posY + 2) <= 6) {
                        playProcessor.next();
                        break;
                    }
                }
            }
        });
    }

    public void initialize() {
        if(Root.tracksHistory != null) {
            Root.tracksHistory.setOnAction((e) -> {
                if(playProcessor.getTrackHistoryGlobal() != null) {
                    playProcessor.getTrackHistoryGlobal().getTrackHistoryContextMenu().show(Root.stage.getScene().getWindow());
                }
            });
        }
    }
}