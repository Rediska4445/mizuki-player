package rf.ebanina.ebanina.Player.Controllers.Playlist;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.DataTypes;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.ebanina.Player.AudioVolumer;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.ebanina.Player.Playlist;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.ebanina.Player.TrackHistory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import static rf.ebanina.File.Field.fields;
import static rf.ebanina.File.FileManager.DEFAULT_USER_MUSIC_KEY;
import static rf.ebanina.UI.Root.tracksListView;

/**
 * <h1>PlayProcessor</h1>
 * Класс для управления внутренним состоянием аудиоплеера.
 * <p>
 * {@code PlayProcessor} предназначен для работы с текущим плейлистом и треками,
 * управления воспроизведением и историей треков.
 * Реализован как отдельный компонент, не влияющий на графический интерфейс.
 * Вся внутренняя логика завязана на JavaFX свойствах для удобства связывания с UI.
 * </p>
 * <p>
 * Особенности:
 * <ul>
 *   <li>Управляет текущим плейлистом и треками.</li>
 *   <li>Поддержка истории воспроизведения треков (TrackHistory).</li>
 *   <li>Использует {@link javafx.beans.property.IntegerProperty} для отслеживания текущих индексов трека и плейлиста.</li>
 *   <li>Обработка изменений трека с сохранением состояния в файл и вызовами обновления UI.</li>
 * </ul>
 * </p>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * // Инициализация с использованием пути по умолчанию
 * PlayProcessor<Track, Playlist> processor = PlayProcessor.playProcessor;
 *
 * // Открыть трек для воспроизведения
 * processor.open(selectedTrack);
 *
 * // Переключиться на следующий трек
 * processor.next();
 *
 * // Переключиться на предыдущий трек
 * processor.down();
 * }</pre>
 *
 * @param <T> тип элементов треков (например, {@link Track})
 * @param <J> тип элементов плейлистов (например, {@link Playlist})
 *
 * @see javafx.beans.property.IntegerProperty
 * @see Track
 * @see Playlist
 * @see TrackHistory
 *
 * @version 0.1.4.5
 * @since 0.1.4.2
 * @author Ebanina Std.
 */
public class PlayProcessor<T extends Track, J extends Playlist>
        implements IPlayController
{
    /**
     * Создаёт экземпляр {@code PlayProcessor} с путём по умолчанию из конфигурации.
     * <p>
     * <b>Логика выбора пути:</b>
     * <ul>
     *   <li>Читает {@code "default_playlist"} из {@link ConfigurationManager}.</li>
     *   <li>Если путь равен {@link FileManager#DEFAULT_USER_MUSIC_KEY} или не существует →
     *       возвращает путь по умолчанию {@link FileManager#getDefaultMusicPath()}.</li>
     *   <li>Иначе использует путь из конфигурации.</li>
     * </ul>
     * </p>
     * <p>
     * Вызывается автоматически при инициализации {@link #playProcessor}.
     * </p>
     */
    private static PlayProcessor<Track, Playlist> build() {
        String path = ConfigurationManager.instance.getItem("default_playlist", FileManager.instance.getDefaultMusicPath());

        if(path.equalsIgnoreCase(DEFAULT_USER_MUSIC_KEY) || !Files.exists(Path.of(path))) {
            return new PlayProcessor<>(FileManager.instance.getDefaultMusicPath(), ConfigurationManager.instance);
        } else {
            return new PlayProcessor<>(path, ConfigurationManager.instance);
        }
    }

    /**
     * Глобальный синглтон экземпляр процессора воспроизведения.
     * <p>
     * Инициализируется лениво через {@link #build()} при первом обращении.
     * Все компоненты плеера работают с этой единственной инстанцией.
     * </p>
     * <p>
     * <b>Использование:</b> {@code PlayProcessor.playProcessor.next();}
     * </p>
     */
    public static PlayProcessor<Track, Playlist> playProcessor = build();

    /**
     * Глобальная история воспроизведения всех треков.
     * <p>
     * Используется для реализации "предыдущий трек" в режиме shuffle
     * ({@link #down()}) через {@link TrackHistory#back()}.
     * </p>
     */
    private TrackHistory trackHistoryGlobal;

    /**
     * Путь к папке музыки по умолчанию (настраиваемый).
     * <p>
     * JavaFX Property для привязки к UI. Может отличаться от {@link #currentMusicDir}
     * (текущая папка плейлиста).
     * </p>
     */
    public StringProperty currentDefaultMusicDir;

    /**
     * Список всех доступных плейлистов (папок с музыкой).
     * <p>
     * Заполняется извне (например, {@link PlaylistController}).
     * </p>
     */
    private ArrayList<J> currentPlaylist = new ArrayList<>();

    /**
     * Текущая активная папка-плейлист (JavaFX Property).
     * <p>
     * Изменяется при переключении плейлистов через {@link PlaylistController#setPlaylist(String)}.
     * </p>
     */
    private final StringProperty currentMusicDir = new SimpleStringProperty();

    /**
     * Текущий список треков плейлиста (ObservableList для UI binding).
     * <p>
     * Автоматически синхронизируется с {@link rf.ebanina.UI.Root#tracksListView}.
     * </p>
     */
    private final ObservableList<T> currentTracksList = FXCollections.observableArrayList();

    /**
     * Индекс текущего воспроизводимого трека (JavaFX Property).
     * <p>
     * Диапазон: [0, {@link #currentTracksList}.size()-1].
     * </p>
     */
    private final IntegerProperty currentTrackIter = new SimpleIntegerProperty();

    /**
     * Индекс текущего плейлиста в {@link #currentPlaylist} (JavaFX Property).
     * <p>
     * Используется {@link PlaylistController} для навигации по плейлистам.
     * </p>
     */
    private final IntegerProperty playlistIter = new SimpleIntegerProperty();

    /**
     * Менеджер конфигурации (инжектируется в конструктор).
     * <p>
     * Используется для чтения настроек воспроизведения (shuffle, mood matching и т.д.).
     * </p>
     */
    private final ConfigurationManager configurationManager;

    /**
     * Конструктор с явной инжекцией ConfigurationManager.
     * <p>
     * Для DI-контейнеров или ручной настройки.
     * </p>
     *
     * @param currentDefaultMusicDir начальная папка музыки
     * @param configurationManager   инжектируемый менеджер настроек
     */
    public PlayProcessor(String currentDefaultMusicDir, ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
        this.currentDefaultMusicDir = new SimpleStringProperty(currentDefaultMusicDir);
    }

    /**
     * Конструктор с глобальным ConfigurationManager.instance.
     * <p>
     * Используется в {@link #build()} для автоматической инициализации.
     * </p>
     *
     * @param currentDefaultMusicDir начальная папка музыки
     */
    public PlayProcessor(String currentDefaultMusicDir) {
        this.configurationManager = ConfigurationManager.instance;
        this.currentDefaultMusicDir = new SimpleStringProperty(currentDefaultMusicDir);
    }

    /**
     * Индекс предыдущего трека (для UI навигации "назад").
     * <p>
     * Устанавливается автоматически обработчиком {@link #trackChangeHandle} при смене трека.
     * </p>
     */
    public IntegerProperty previousIndex = new SimpleIntegerProperty(0);

    /**
     * Возвращает индекс предыдущего трека.
     */
    public int getPreviousIndex() {
        return previousIndex.get();
    }

    /**
     * JavaFX Property для индекса предыдущего трека (UI binding).
     */
    public IntegerProperty previousIndexProperty() {
        return previousIndex;
    }

    /**
     * Устанавливает индекс предыдущего трека.
     */
    public void setPreviousIndex(int previousIndex) {
        this.previousIndex.set(previousIndex);
    }

    /**
     * Возвращает путь к папке музыки по умолчанию.
     */
    public String getCurrentDefaultMusicDir() {
        return currentDefaultMusicDir.get();
    }

    /**
     * JavaFX Property для пути по умолчанию (UI binding).
     */
    public StringProperty currentDefaultMusicDirProperty() {
        return currentDefaultMusicDir;
    }

    /**
     * Устанавливает путь к папке музыки по умолчанию.
     */
    public void setCurrentDefaultMusicDir(String currentDefaultMusicDir) {
        this.currentDefaultMusicDir.set(currentDefaultMusicDir);
    }

    /**
     * Флаг режима сетевых (онлайн) треков.
     * <p>
     * Влияет на логику сохранения статистики в {@link #trackChangeHandle}.
     * </p>
     */
    public SimpleBooleanProperty networkProperty = new SimpleBooleanProperty(false);

    /**
     * Проверяет активен ли режим сетевых треков.
     */
    public boolean isNetwork() {
        return networkProperty.get();
    }

    /**
     * JavaFX Property для режима сети.
     */
    public SimpleBooleanProperty networkPropertyProperty() {
        return networkProperty;
    }

    /**
     * Устанавливает режим сетевых треков.
     */
    public void setNetwork(boolean networkProperty) {
        this.networkProperty.set(networkProperty);
    }

    /**
     * Карта обработчиков событий смены трека.
     * <p>
     * <b>По умолчанию содержит:</b>
     * <ul>
     *   <li>{@code "main"}: сохранение статистики (tempo/pan/volume, время прослушивания,
     *       количество полных прослушиваний, общее время) + добавление в {@link #trackHistoryGlobal}.</li>
     * </ul>
     * Вызывается в {@link #open(Track)}, {@link #next()}, {@link #down()}.
     * </p>
     */
    private final Map<String, Consumer<Track>> trackChangeHandle = new HashMap<>(Map.of(
            "main", new Consumer<Track>() {
                @Override
                public void accept(Track track) {
                    String p;
                    String trackType = track.getPath();

                    // FIXME: Время для сетевого трека

                    if(playProcessor.isNetwork()) {
                        p = Path.of(
                                Resources.Properties.DEFAULT_INET_TRACKS_CACHE_PATH.getKey()
                        ).toAbsolutePath().toString();

                        trackType = track.getViewName();
                    } else {
                        p = Path.of(
                                Resources.Properties.DEFAULT_CACHE_TRACKS_PATH.getKey(),
                                FileManager.instance.name(currentPlaylist.get(playlistIter.get()).getName())
                        ).toAbsolutePath().toString();
                    }

                    Map<String, String> read = FileManager.instance.readArray(
                            p,
                            track.getPath(),
                            Map.of()
                    );

                    String timestamp = LocalDateTime.now()
                            .truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                    Map<String, String> timestampMap = new HashMap<>(Map.ofEntries(
                            Map.entry(fields.get(DataTypes.TEMPO.code).getLocalName(), String.valueOf((double) MediaProcessor.mediaProcessor.mediaPlayer.getTempo())),
                            Map.entry(fields.get(DataTypes.PAN.code).getLocalName(), String.valueOf((double) MediaProcessor.mediaProcessor.mediaPlayer.getPan())),
                            Map.entry(fields.get(DataTypes.VOLUME.code).getLocalName(), String.valueOf(MediaProcessor.mediaProcessor.mediaPlayer.getVolume())),
                            Map.entry(DataTypes.COUNT_STREAM.code, "1"),
                            Map.entry("timestamp", timestamp)
                    ));

                    Map<String, String> save = new HashMap<>(Map.ofEntries(
                            Map.entry(fields.get(DataTypes.TEMPO.code).getLocalName(), String.valueOf((double) MediaProcessor.mediaProcessor.mediaPlayer.getTempo())),
                            Map.entry(fields.get(DataTypes.PAN.code).getLocalName(), String.valueOf((double) MediaProcessor.mediaProcessor.mediaPlayer.getPan())),
                            Map.entry(fields.get(DataTypes.VOLUME.code).getLocalName(), String.valueOf(MediaProcessor.mediaProcessor.mediaPlayer.getVolume())),
                            Map.entry(DataTypes.LAST_DATE.code, String.valueOf(
                                    LocalDateTime.now()
                            )),
                            Map.entry(DataTypes.COUNT_STREAM.code, String.valueOf(Integer.parseInt(
                                    read.getOrDefault(DataTypes.COUNT_STREAM.code, "0")) + 1))
                    ));

                    if(MediaProcessor.mediaProcessor.mediaPlayer.getCurrentTime().toSeconds() < MediaProcessor.mediaProcessor.mediaPlayer.getOverDuration().toSeconds() - 30) {
                        save.put(fields.get(DataTypes.TIME.code).getLocalName(), String.valueOf((int) MediaProcessor.mediaProcessor.mediaPlayer.getCurrentTime().toSeconds()));

                        timestampMap.put("is full play", "false");
                    } else {
                        save.put(DataTypes.COUNT_FULLY_PLAY.code, String.valueOf(Integer.parseInt(
                                read.getOrDefault(DataTypes.COUNT_FULLY_PLAY.code, "0")) + 1));

                        timestampMap.put("is full play", "true");
                    }

                    save.put(DataTypes.TOTAL_TIME_PLAYED.code, String.valueOf(Float.parseFloat(read.getOrDefault(DataTypes.TOTAL_TIME_PLAYED.code, "0")) + MediaProcessor.mediaProcessor.mediaPlayer.getCurrentTime().toSeconds()));

                    timestampMap.put(fields.get(DataTypes.TIME.code).getLocalName(), String.valueOf((int) MediaProcessor.mediaProcessor.mediaPlayer.getCurrentTime().toSeconds()));

                    FileManager.instance.saveArray(
                            p,
                            trackType,
                            save
                    );

                    timestampMap.put("pc_volume", String.valueOf(AudioVolumer.instance.getSystemVolume()));

                    FileManager.instance.saveCollection(
                            p,
                            "time_array",
                            System.currentTimeMillis() + " - " + trackType,
                            timestampMap
                    );

                    previousIndex.set(currentTrackIter.get());

                    trackHistoryGlobal.add(currentTracksList.get(currentTrackIter.get()));
                }
            }
    ));

    public Map<String, Consumer<Track>> getTrackChangeHandle() {
        return trackChangeHandle;
    }

    /**
     * Открывает и запускает воспроизведение указанного трека.
     * <p>
     * Метод обеспечивает корректную работу с треками:
     * <ul>
     *   <li>Если индекс текущего трека вне диапазона — устанавливается 0.</li>
     *   <li>При воспроизведении сетевого трека (определяется isNetwork()) обновляет список треков из UI и запускает воспроизведение.</li>
     *   <li>Для локальных треков проверяет директорию и при смене загружает новые треки с помощью {@link FileManager#getMusic}. Обновляет текущий плейлист и вызывает обновление UI через {@link PlaylistController}.</li>
     *   <li>Устанавливает индекс текущего трека и вызывает обновление UI воспроизведения {@link MediaProcessor#_track}.</li>
     * </ul>
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * playProcessor.open(track); // открыть и воспроизвести трек
     * }</pre>
     * <h2>Особенности</h2>
     * <ul>
     *   <li>Обрабатывает локальные и сетевые треки по-разному.</li>
     *   <li>Гарантирует корректную инициализацию списка треков и плейлиста.</li>
     * </ul>
     * @param newValue трек для открытия и воспроизведения
     * @see FileManager#getMusic
     * @see PlaylistController
     * @see MediaProcessor#_track
     */
    @Override
    public void open(Track newValue) {
        if (currentTrackIter.get() < 0 || currentTrackIter.get() >= currentTracksList.size()) {
            currentTrackIter.set(0);
        }

        trackChangeHandle.get("main").accept(getTracks().get(playProcessor.getTrackIter()));

        if(!newValue.isNetty()) {
            if (!getCurrentMusicDir().equals(newValue.getFilePath().getParent().toString())) {
                currentTracksList.clear();

                try {
                    playProcessor.getTracks().addAll(FileManager.instance.getMusic(newValue.getFilePath().getParent()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                PlayProcessor.playProcessor.setCurrentPlaylistIter(PlayProcessor.playProcessor.getCurrentPlaylist().indexOf(
                        new Playlist(Path.of(newValue.getPath()).getParent().toString())
                ));

                PlayProcessor.playProcessor.setCurrentMusicDir(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath());

                PlaylistController.playlistController.onPlaylistChanged.run();

                Platform.runLater(() -> tracksListView.getTrackListView().getSelectionModel().select(PlayProcessor.playProcessor.getTrackIter()));
            }
        }

        int index = PlayProcessor.playProcessor.getTracks().indexOf(newValue);

        if(index > -1) {
            PlayProcessor.playProcessor.setTrackIter(index);
        }

        MediaProcessor.mediaProcessor._track();
    }

    public Random trackRandomizer = new Random();

    /**
     * Выполняет действие переключения на следующий трек в плейлисте.
     * <p>
     * Метод обновляет состояние, вызывая обработчик смены трека {@link PlayProcessor#getTrackChangeHandle()}.
     * Реализована логика обхода "фантомных" треков, чтобы пропустить их при переключении.
     * <p>
     * Логика переключения выбирается в зависимости от настроек:
     * <ul>
     *   <li>Если алгоритм подбора треков настроен как "без настроения", переключается на следующий трек по порядку.</li>
     *   <li>Если включён случайный режим (микширования), выбирается случайный трек из плейлиста.</li>
     *   <li>Если алгоритм настроен на подстройку под настроение, переключается на следующий трек по порядку.</li>
     * </ul>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * playProcessor.next(); // переключиться на следующий трек
     * }</pre>
     * <h2>Особенности</h2>
     * <ul>
     *   <li>Пропускает фантомные треки.</li>
     *   <li>Учитывает настройки микширования и алгоритма подбора треков из конфигурации.</li>
     * </ul>
     */
    @Override
    public void next() {
        trackChangeHandle.get("main").accept(currentTracksList.get(currentTrackIter.get()));

        if(!MediaProcessor.mediaProcessor.isAutoPlayback()) {
            if (!configurationManager.getBooleanItem("use_mood_matching_algorithm", "false")) {
                if (MediaProcessor.mediaProcessor.getPlayRandomProperty().getValue().equals(true)) {
                    currentTrackIter.set(trackRandomizer.nextInt(0, currentTracksList.size() - 1));
                } else {
                    currentTrackIter.set(currentTrackIter.get() + 1);
                }
            } else {
                currentTrackIter.set(currentTrackIter.get() + 1);
            }

            if(currentTrackIter.get() < currentTracksList.size()) {
                if (currentTracksList.get(currentTrackIter.get()).isPhantom()) {
                    while (currentTracksList.get(currentTrackIter.get()).isPhantom()) {
                        currentTrackIter.set(currentTrackIter.get() + 1);
                    }
                }
            }
        }

        MediaProcessor.mediaProcessor._track();
    }

    /**
     * Переключает воспроизведение на предыдущий трек в текущем плейлисте.
     * <p>
     * Метод корректно работает в различных сценариях:
     * <ul>
     *   <li>Если включён режим случайного воспроизведения (микширования), выбирает последний трек из истории воспроизведения {@link TrackHistory}.</li>
     *   <li>Если случайный режим выключен, переключается на трек с индексом меньше текущего на единицу.</li>
     * </ul>
     * <p>
     * При переключении вызывается обработчик изменения трека {@link PlayProcessor#getTrackChangeHandle()}, который сохраняет состояние и обновляет историю.
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * playProcessor.down(); // переключиться на предыдущий трек
     * }</pre>
     * <h2>Особенности</h2>
     * <ul>
     *   <li>Обеспечивает корректное обновление состояния при смене треков.</li>
     *   <li>Учитывает режим случайного воспроизведения и историю треков.</li>
     * </ul>
     * @see PlayProcessor#getTrackChangeHandle()
     * @see TrackHistory
     */
    @Override
    @SuppressWarnings("SuspiciousMethodCalls") // Список истории мал, и обычно вызывается редко
    public void down() {
        trackChangeHandle.get("main").accept(getCurrentTrack());

        if (MediaProcessor.mediaProcessor.getPlayRandomProperty().get()) {
            setTrackIter(currentTracksList.indexOf(trackHistoryGlobal.back()));
        } else {
            setTrackIter(currentTrackIter.get() - 1);
        }

        MediaProcessor.mediaProcessor._track();
    }

    /**
     * JavaFX Property индекса текущего трека.
     */
    public IntegerProperty getTrackIterProperty() {
        return currentTrackIter;
    }

    /**
     * Возвращает индекс текущего трека.
     */
    public int getTrackIter() {
        return currentTrackIter.get();
    }

    /**
     * Устанавливает индекс трека (Fluent API).
     */
    public PlayProcessor<T, J> setTrackIter(int trackIter) {
        this.currentTrackIter.set(trackIter);
        return this;
    }

    /**
     * Возвращает индекс текущего плейлиста.
     */
    public int getCurrentPlaylistIter() {
        return playlistIter.get();
    }

    /**
     * Устанавливает индекс плейлиста (Fluent API).
     */
    public PlayProcessor<T, J> setCurrentPlaylistIter(int playlistIter) {
        this.playlistIter.set(playlistIter);
        return this;
    }

    /**
     * Возвращает текущий воспроизводимый трек.
     */
    public Track getCurrentTrack() {
        return currentTracksList.get(currentTrackIter.get());
    }

    /**
     * Безопасный доступ к треку по индексу с дефолтным значением.
     */
    public Track getOrDefault(int index, Track defaultValue) {
        if(currentTracksList.isEmpty())
            return null;

        if(index < 0 || index > currentTracksList.size() - 1) {
            return defaultValue;
        }

        return currentTracksList.get(index);
    }

    /**
     * Безопасный доступ к треку (null-safe) с дефолтным значением.
     */
    public Track getOrNonNullDefault(int index, Track defaultValue) {
        if(currentTracksList.isEmpty())
            return null;

        if(index < 0 || index > currentTracksList.size() - 1) {
            return defaultValue;
        }

        if(currentTracksList.get(index) == null)
            return defaultValue;

        return currentTracksList.get(index);
    }

    /**
     * Возвращает ObservableList текущих треков (для UI binding).
     */
    public ObservableList<T> getTracks() {
        return currentTracksList;
    }

    /**
     * Заменяет весь список треков (ObservableList).
     */
    public PlayProcessor<T, J> setTracks(ObservableList<T> tracks) {
        this.currentTracksList.setAll(tracks);
        return this;
    }

    /**
     * Заменяет список треков (ArrayList → ObservableList).
     */
    public PlayProcessor<T, J> setTracks(ArrayList<T> tracks) {
        this.currentTracksList.clear();
        this.currentTracksList.addAll(tracks);
        return this;
    }

    /**
     * Возвращает список всех плейлистов.
     */
    public ArrayList<J> getCurrentPlaylist() {
        return currentPlaylist;
    }

    /**
     * Возвращает путь текущей папки-плейлиста.
     */
    public String getCurrentMusicDir() {
        return currentMusicDir.get();
    }

    /**
     * JavaFX Property текущей папки (UI binding).
     */
    public StringProperty currentMusicDirProperty() {
        return currentMusicDir;
    }

    /**
     * Устанавливает текущую папку-плейлист (Fluent API).
     */
    public PlayProcessor<T, J> setCurrentMusicDir(String currentMusicDir) {
        this.currentMusicDir.set(currentMusicDir);
        return this;
    }

    /**
     * Устанавливает список всех плейлистов (Fluent API).
     */
    public PlayProcessor<T, J> setCurrentPlaylist(ArrayList<J> currentPlaylist) {
        this.currentPlaylist = currentPlaylist;
        return this;
    }

    /**
     * Возвращает глобальную историю треков.
     */
    public TrackHistory getTrackHistoryGlobal() {
        return trackHistoryGlobal;
    }

    /**
     * Устанавливает глобальную историю треков (Fluent API).
     */
    public PlayProcessor<T, J> setTrackHistoryGlobal(TrackHistory trackHistoryGlobal) {
        this.trackHistoryGlobal = trackHistoryGlobal;
        return this;
    }

    public ObservableList<T> getCurrentTracksList() {
        return currentTracksList;
    }

    public int getCurrentTrackIter() {
        return currentTrackIter.get();
    }

    public IntegerProperty currentTrackIterProperty() {
        return currentTrackIter;
    }

    public void setCurrentTrackIter(int currentTrackIter) {
        this.currentTrackIter.set(currentTrackIter);
    }

    public int getPlaylistIter() {
        return playlistIter.get();
    }

    public IntegerProperty playlistIterProperty() {
        return playlistIter;
    }

    public void setPlaylistIter(int playlistIter) {
        this.playlistIter.set(playlistIter);
    }
}