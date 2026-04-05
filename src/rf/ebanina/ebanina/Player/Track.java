package rf.ebanina.ebanina.Player;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.util.Duration;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.DataTypes;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Metadata.MetadataOfFile;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.Network.Info;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.utils.collections.TypicalMapWrapper;
import rf.ebanina.utils.loggining.Prefix;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static rf.ebanina.Network.Info.playersMap;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.size;

/**
 * <h1>Track</h1>
 * Универсальное представление трека — локального или сетевого.
 * <p>
 * <h2>Ключевые возможности:</h2>
 * <ul>
 *   <li><b>Двойная природа:</b> локальные файлы ({@code path} = файловая строка)
 *       и сетевые треки ({@code path} = URL строка, {@link #isNetty} = true).</li>
 *   <li><b>Ленивая загрузка метаданных:</b> title/artist/duration загружаются только при первом обращении.</li>
 *   <li><b>Кэширование обложек:</b> {@link #albumArt}, {@link #mipmap} с поддержкой разных размеров.</li>
 *   <li><b>Состояние воспроизведения:</b> время остановки, статистика прослушиваний.</li>
 *   <li><b>Теги:</b> пользовательские метки {@link Tag} с сериализацией.</li>
 *   <li><b>Serializable + Cloneable:</b> для сохранения в кэш/сериализацию.</li>
 * </ul>
 * </p>
 *
 * <h2>Создание</h2>
 * <pre>{@code
 * Track local = new Track("/music/song.mp3");     // локальный
 * Track online = new Track(new URL("http://...")); // сетевой
 * }</pre>
 *
 * @see MetadataOfFile для загрузки метаданных
 * @see MediaReference для интеграции с MediaPlayer
 * @implements Serializable, Cloneable, Comparable&lt;Track&gt;
 */
public class Track
        implements Serializable, Cloneable, MediaReference, Comparable<Track>
{
    /**
     * <h1>Tag</h1>
     * Простая пользовательская метка (тег) для классификации треков.
     * <p>
     * Используется в {@link Track#getTags()} для создания персональных плейлистов
     * типа "rock", "90s", "workout", "favorites".
     * </p>
     * <p>
     * <b>Сериализация:</b> сохраняется как CSV-строка в кэше:
     * <pre>{@code "rock,90s,favorite"}</pre>
     * </p>
     * <p>
     * <b>Сравнение:</b> только по {@code name} (case-sensitive).
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * // Создание тегов
     * Tag rock = new Tag("rock");
     * Tag workout = new Tag("workout").setName("training");
     *
     * // Добавление к треку
     * track.setTags(new ArrayList<>(Arrays.asList(rock, workout)));
     *
     * // Сохранение
     * track.serializeTags(new File("cache/"));
     * }</pre>
     *
     * @see Track#setTags(ArrayList)
     * @see Track#serializeTags(File)
     * @see Track#deserializeTags(File)
     */
    public static class Tag
            implements Serializable
    {
        /**
         * Версия сериализации для {@link Track} (1).
         */
        @Serial
        private static final long serialVersionUID = 2L;

        /**
         * Имя тега ("rock", "90s", "favorite").
         */
        private String name;

        /**
         * Конструктор по умолчанию (пустой тег).
         */
        public Tag() {}

        /**
         * Конструктор с именем тега.
         * @param name название тега
         */
        public Tag(String name) {
            this.name = name;
        }

        /**
         * Устанавливает имя тега (Fluent API).
         * @param name новое название
         * @return {@code this}
         */
        public Tag setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Возвращает имя тега.
         */
        public String getName() {
            return name;
        }

        /**
         * Равенство по {@code name} (стандартная логика).
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tag tag = (Tag) o;
            return Objects.equals(name, tag.name);
        }

        /**
         * HashCode по {@code name}.
         */
        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Максимальный размер кэша треков (настраивается в config).
     * <p>
     * По умолчанию: 25 треков.
     * </p>
     */
    public static int CACHE_SIZE = ConfigurationManager.instance.getItem(int.class, "track_cache_size", 25);

    /**
     * Список пользовательских тегов для классификации трека.
     * <p>
     * Примеры: {@code ["rock", "90s", "favorites", "workout"]}.
     * </p>
     * <p>
     * Сериализуется как CSV-строка в кэше трека:
     * <pre>{@code "rock,90s,favorite"}</pre>
     * </p>
     * <p>
     * <b>Доступ:</b> {@link Track#getTags()} / {@link Track#setTags(ArrayList)}.
     * </p>
     */
    private ArrayList<Tag> tags;

    /**
     * Основной идентификатор объекта Track.
     * <p>
     * <b>Форматы:</b>
     * <ul>
     *   <li><b>Локальный:</b> {@code "/music/artist - song.mp3"}</li>
     *   <li><b>Сетевой:</b> {@code "https://server.com/song.mp3"}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Использование:</b>
     * <ul>
     *   <li>Ключ для {@link #equals(Object)}, {@link #hashCode()}.</li>
     *   <li>Источник для метаданных ({@link #getTitle()}, {@link #getArtist()}).</li>
     *   <li>Путь для {@link MediaPlayer} и кэширования.</li>
     * </ul>
     * </p>
     */
    private String path;

    /**
     * Дополнительная внешняя ссылка (опционально).
     * <p>
     * Используется для:
     * <ul>
     *   <li>Оригинального источника сетевых треков.</li>
     *   <li>Резервной ссылки при проблемах с основной {@link #path}.</li>
     *   <li>Ссылки на страницу трека/альбома.</li>
     * </ul>
     * </p>
     * <p>
     * По умолчанию: {@code null}.
     * </p>
     */
    private String externalUrl = null;

    /**
     * Восстанавливать ли состояние воспроизведения при открытии трека.
     * Настройка из конфига: {@code "restore_state_track"} (по умолчанию {@code false}).
     * </p>
     */
    private boolean restoreState = ConfigurationManager.instance.getBooleanItem("restore_state_track", "false");

    /**
     * Имя плейлиста/папки (кэшируется из parent {@link #path}).
     * <p>
     * <b>Ленивая инициализация:</b> {@link #getPlaylistName()} вычисляет из
     * {@code new File(path).getParentFile().getName()} при первом обращении.
     * </p>
     * <p>
     * Для сетевых треков: {@code playlist = path} (URL как есть).
     * </p>
     */
    private String playlist = null;

    /**
     * Общая длительность трека в секундах (кэшируется).
     * <p>
     * <b>Ленивая загрузка:</b> {@link #getTotalDurationInSeconds()} через
     * {@link MetadataOfFile#getAudioFileDuration(String)}.
     * </p>
     * <p>
     * <b>Значения:</b>
     * <ul>
     *   <li>{@code -1}: не загружено.</li>
     *   <li>{@code ≥0}: секунды.</li>
     * </ul>
     * </p>
     */
    public int totalDuraSec = -1;

    /**
     * Последнее время остановки (секунды, кэшируется).
     * <p>
     * <b>Использование:</b>
     * <ul>
     *   <li>Восстановление позиции при {@link #restoreState} = true.</li>
     *   <li>Сохранение в кэше {@link #getState()}.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Значения:</b>
     * <ul>
     *   <li>{@code -1}: не установлено.</li>
     *   <li>{@code ≥0}: секунды от начала.</li>
     * </ul>
     * </p>
     */
    public int lastTimeTrack = -1;

    /**
     * Обложка альбома (стандартный размер).
     * <p>
     * <b>Ленивая загрузка:</b> {@link #getAlbumArt()} через {@link MetadataOfFile}.
     * </p>
     * <p>
     * Кэшируется до вызова {@link Track#albumArtCleaner(List)}.
     * </p>
     */
    public Image albumArt;

    /**
     * Миниатюра обложки (mipmap, оптимизированный размер).
     * <p>
     * <b>Ленивая загрузка:</b> {@link #getMipmap()} с размером {@link ColorProcessor#size}.
     * </p>
     * <p>
     * Используется для списков треков (экономия памяти по сравнению с {@link #albumArt}).
     * </p>
     */
    public Image mipmap;

    /**
     * Текущее время воспроизведения трека (секунды от начала).
     * <p>
     * Устанавливается вручную или восстанавливается из кэша {@link #getState()}.
     * </p>
     * <p>
     * Используется для {@link #getTimeTrack()} → {@link Duration}.
     * </p>
     */
    private int timeTrack;

    /**
     * Название трека.
     * <p>
     * <b>Ленивая загрузка:</b> {@link #getTitle()} через {@link MetadataOfFile#getTitle(String)}.
     * </p>
     * <p>
     * Используется в {@link #viewName()} для формирования {@code "Artist - Title"}.
     * </p>
     */
    public String title;

    /**
     * Исполнитель/группа.
     * <p>
     * <b>Ленивая загрузка:</b> {@link #getArtist()} через {@link MetadataOfFile#getArtist(String)}.
     * </p>
     * <p>
     * Используется в {@link #viewName()} и сравнении треков ({@link rf.ebanina.ebanina.Player.Controllers.Playlist.PlaylistController}).
     * </p>
     */
    public String artist;

    /**
     * Отображаемое имя трека: {@code "Artist - Title"}.
     * <p>
     * <b>Ленивая инициализация:</b> {@link #viewName()} вычисляет при первом обращении.
     * </p>
     * <p>
     * Fallback: имя файла без расширения, если метаданные недоступны.
     * </p>
     */
    public String viewName;

    /**
     * Флаг сетевого (онлайн) трека.
     * <p>
     * <b>Локальный:</b> {@code false} (конструктор {@link Track#Track(String)}).
     * <b>Сетевой:</b> {@code true} (конструктор {@link Track#Track(URL)}).
     * </p>
     * <p>
     * Влияет на:
     * <ul>
     *   <li>Сохранение статистики ({@link PlayProcessor#getTrackChangeHandle()}).</li>
     *   *   <li>Восстановление времени ({@link #getRawLastTimeTrack()}).</li>
     *   <li>Логику {@link PlayProcessor#open(Track)}.</li>
     * </ul>
     * </p>
     */
    private boolean isNetty = false;

    /**
     * "Фантомный" трек — пропускается автоматически в {@link PlayProcessor#next()}.
     * <p>
     * <b>Применение:</b>
     * <ul>
     *   <li>Повреждённые/недоступные файлы.</li>
     *   <li>Треки с ошибками метаданных.</li>
     *   <li>Временная разметка для фильтрации.</li>
     * </ul>
     * </p>
     */
    public boolean isPhantom = false;

    /**
     * Кэшированные метаданные трека (статистика воспроизведения).
     * <p>
     * <b>Содержит:</b>
     * <ul>
     *   <li>{@code tempo}, {@code pan}, {@code volume} (текущие настройки).</li>
     *   <li>{@code count_stream}, {@code count_fully_play} (статистика).</li>
     *   <li>{@code total_time_played}, {@code time} (время прослушивания).</li>
     *   <li>{@code last_date} (дата последнего воспроизведения).</li>
     * </ul>
     * </p>
     * <p>
     * Лениво загружается из кэша через {@link PlayProcessor#getTrackChangeHandle()}.
     * </p>
     */
    public TypicalMapWrapper<String> metadata = new TypicalMapWrapper<>();

    /**
     * Конструктор по умолчанию.
     * <p>
     * Создаёт пустой трек без пути и метаданных.
     * </p>
     * <p>
     * <b>Использование:</b> десериализация, временные объекты.
     * </p>
     */
    public Track() {}

    /**
     * <h3>Локальный трек</h3>
     * Создаёт трек для mp3-файла на диске.
     * <p>
     * <b>Инициализация:</b>
     * <ul>
     *   <li>{@link #path} = переданный путь.</li>
     *   <li>{@link #isNetty} = {@code false}.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Пример:</b>
     * <pre>{@code
     * Track local = new Track("/music/Artist - Song.mp3");
     * // Лениво загрузятся: title, artist, duration, обложка
     * System.out.println(local.viewName()); // "Artist - Song"
     * }</pre>
     * </p>
     *
     * @param path путь к mp3-файлу
     */
    public Track(String path) {
        this.path = path;
        this.isNetty = false;
    }

    /**
     * <h3>Сетевой трек</h3>
     * Создаёт трек для онлайн-источника (URL).
     * <p>
     * <b>Инициализация:</b>
     * <ul>
     *   <li>{@link #path} = строковое представление URL.</li>
     *   <li>{@link #isNetty} = {@code true}.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Особенности сетевых треков:</b>
     * <ul>
     *   <li>Без кэширования времени остановки ({@link #getRawLastTimeTrack()} → 0).</li>
     *   <li>Сохранение статистики в отдельный кэш ({@link Resources.Properties#DEFAULT_INET_TRACKS_CACHE_PATH}).</li>
     * </ul>
     * </p>
     * <p>
     * <b>Пример:</b>
     * <pre>{@code
     * Track online = new Track(new URL("https://server.com/song.mp3"));
     * // Лениво загрузятся: title, artist (если поддерживается сервером)
     * }</pre>
     * </p>
     *
     * @param path URL трека
     */
    public Track(URL path) {
        this.path = path.toString();
        this.isNetty = true;
    }

    /**
     * <h3>Формирует отображаемое имя трека</h3>
     * Лениво вычисляет {@code "Artist - Title"} для UI-списков.
     * <p>
     * <b>Алгоритм (при {@link #viewName} = null/пусто):</b>
     * <ol>
     *   <li>Загружает {@link #artist} и {@link #title} из метаданных.</li>
     *   <li>Если метаданные недоступны → имя файла без расширения.</li>
     *   <li>Кэширует результат в {@link #viewName}.</li>
     * </ol>
     * </p>
     * <p>
     * <b>Примеры:</b>
     * <pre>
     * "The Beatles - Yesterday"     ← метаданные
     * "song.mp3" → "song"           ← fallback
     * </pre>
     * </p>
     * <p>
     * Используется для сортировки ({@link #compareTo(Track)}) и отображения.
     * </p>
     *
     * @return готовое имя для UI или имя файла
     */
    public String viewName() {
        if(viewName == null || viewName.equals("")) {
            artist = getArtist();
            title = getTitle();

            if (artist == null || title == null)
                if(!isNetty)
                    return getName(getPath().substring(getPath().lastIndexOf(".")));

            viewName = artist + " - " + title;
        }

        return viewName;
    }

    /**
     * Проверяет, является ли трек сетевым (онлайн).
     * <p>
     * Устанавливается в конструкторах:
     * <ul>
     *   <li>{@link Track#Track(String)} → {@code false}</li>
     *   <li>{@link Track#Track(URL)} → {@code true}</li>
     * </ul>
     * </p>
     *
     * @return {@code true} для онлайн-треков
     */
    public boolean isNetty() {
        return isNetty;
    }

    /**
     * Устанавливает флаг сетевого трека (Fluent API).
     * <p>
     * <b>Влияние:</b>
     * <ul>
     *   <li>Локальный → сетевой: статистика в {@link Resources.Properties#DEFAULT_INET_TRACKS_CACHE_PATH}.</li>
     *   <li>Сетевой → локальный: обычный кэш файлов.</li>
     * </ul>
     * </p>
     *
     * @param netty новый статус
     * @return {@code this} для цепочки вызовов
     */
    public Track setNetty(boolean netty) {
        isNetty = netty;
        return this;
    }

    /**
     * Устанавливает кэшированную обложку альбома (Fluent API).
     * <p>
     * Перезаписывает {@link #albumArt}. Следующие вызовы {@link #getAlbumArt()}
     * будут возвращать эту картинку без обращения к {@link MetadataOfFile}.
     * </p>
     *
     * @param albumArt готовая Image обложки
     * @return {@code this}
     */
    public Track setAlbumArt(Image albumArt) {
        this.albumArt = albumArt;
        return this;
    }

    /**
     * Устанавливает кэшированную миниатюру обложки (Fluent API).
     * <p>
     * Перезаписывает {@link #mipmap}. Используется для списков треков.
     * </p>
     *
     * @param mipmap готовая Image мипмапа
     * @return {@code this}
     */
    public Track setMipmap(Image mipmap) {
        this.mipmap = mipmap;
        return this;
    }

    /**
     * <h3>Ленивая загрузка мипмапа</h3>
     * Минималистичная обложка для списков.
     * <p>
     * <b>Логика:</b>
     * <ul>
     *   <li>Если {@link #mipmap} = null → создаёт через {@link #getIndependentAlbumArt(int, int, boolean, boolean)} ()}.</li>
     *   <li>Кэширует результат в {@link #mipmap}.</li>
     * </ul>
     * </p>
     *
     * @param size размер мипмапа (квадратный)
     * @return готовый Image
     */
    public Image getMipmap(int size) {
        return mipmap == null ? mipmap = getIndependentMipmap(size) : mipmap;
    }

    /**
     * <h3>Загрузка мипмапа</h3>
     * Минималистичная обложка для списков.
     *
     * @param size размер мипмапа (квадратный)
     * @return готовый Image
     */
    public Image getIndependentMipmap(int size) {
        return getIndependentAlbumArt(size, size, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth);
    }

    /**
     * Получает обложку напрямую из метаданных (НЕ кэширует).
     * <p>
     * Обходит кэш {@link #albumArt}/{@link #mipmap}, всегда свежие данные.
     * </p>
     * <p>
     * <b>Параметры:</b>
     * <ul>
     *   <li>{@code size, size1}: размеры (width, height).</li>
     *   <li>{@code a}: preserve aspect ratio.</li>
     *   <li>{@code a1}: smooth scaling.</li>
     * </ul>
     * </p>
     *
     * @return Image обложки заданных параметров
     * @see MetadataOfFile#getArt(Track, int, int, boolean, boolean)
     */
    public Image getIndependentAlbumArt(int size, int size1, boolean a, boolean a1) {
        return MetadataOfFile.iMetadataOfFiles.getArt(this, size, size1, a, a1);
    }

    /**
     * <h3>Ленивая загрузка стандартной обложки</h3>
     * Полная обложка альбома с кэшированием.
     * <p>
     * <b>Логика:</b>
     * <ul>
     *   <li>Если {@link #albumArt} = null → создаёт и кэширует.</li>
     *   <li>Возвращает из кэша.</li>
     * </ul>
     * </p>
     *
     * @param size  ширина
     * @param size1 высота
     * @param a     preserve aspect ratio
     * @param a1    smooth scaling
     * @return Image обложки
     */
    public Image getAlbumArt(int size, int size1, boolean a, boolean a1) {
        return albumArt == null ? albumArt = getIndependentAlbumArt(size, size1, a, a1) : albumArt;
    }

    /**
     * Обложка квадратного размера со стандартными настройками.
     * <p>
     * Использует параметры {@link ColorProcessor}: {@code isPreserveRatio}, {@code isSmooth}.
     * </p>
     *
     * @param size размер (width = height)
     * @return Image обложки
     */
    public Image getAlbumArt(int size) {
        return getAlbumArt(size, size, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth);
    }

    /**
     * Обложка стандартного размера ({@link ColorProcessor#size}).
     * <p>
     * Самый частый вызов для детального просмотра трека.
     * </p>
     *
     * @return Image обложки
     */
    public Image getAlbumArt() {
        return getAlbumArt(size);
    }

    /**
     * <h3>Очиститель кэша обложек</h3>
     * Освобождает память от {@link #albumArt} для списка треков.
     * <p>
     * <b>Зачем нужно:</b>
     * <ul>
     *   <li>Списки из 100+ треков с обложками жрут много RAM.</li>
     *   <li>Вызывать перед скроллом ListView или при смене плейлиста.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Не трогает:</b> {@link #mipmap} (миниатюры остаются).
     * </p>
     *
     * @param input список треков для очистки
     * @see Track#setAlbumArt(Image)
     */
    public static void albumArtCleaner(List<Track> input) {
        for(Track a : input)
            a.setAlbumArt(null);
    }

    /**
     * <h3>Ленивая загрузка длительности трека</h3>
     * Возвращает общую длительность в секундах с кэшированием.
     * <p>
     * <b>Логика:</b>
     * <ul>
     *   <li>{@link #totalDuraSec} ≤ 0 → загружает из {@link MetadataOfFile} и кэширует.</li>
     *   <li>Иначе → возвращает из кэша.</li>
     * </ul>
     * </p>
     * <p>
     * Используется для {@link #getFormattedTotalDuration()} и UI.
     * </p>
     *
     * @return длительность в секундах (≥ 0)
     */
    public int getTotalDurationInSeconds() {
        return totalDuraSec <= 0 ? totalDuraSec = MetadataOfFile.iMetadataOfFiles.getAudioFileDuration(path) : totalDuraSec;
    }

    /**
     * Кэш состояния трека (статистика воспроизведения).
     * <p>
     * Лениво заполняется {@link #getState()} из файлового кэша.
     * </p>
     */
    private final Map<String, String> state = new HashMap<>();

    /**
     * <h3>Ленивая загрузка статистики трека</h3>
     * Читает сохранённое состояние из кэша при первом обращении.
     * <p>
     * <b>Путь к кэшу:</b>
     * <pre>{@code /cache/tracks/{playlistName}/{trackPath}.data}</pre>
     * </p>
     * <p>
     * <b>Содержимое:</b>
     * <ul>
     *   <li>{@code tempo}, {@code pan}, {@code volume}.</li>
     *   <li>{@code count_stream}, {@code count_fully_play}.</li>
     *   <li>{@code total_time_played}, {@code time}, {@code last_date}.</li>
     * </ul>
     * </p>
     * <p>
     * Кэшируется в {@link #state} до конца жизни объекта.
     * </p>
     *
     * @return Map со статистикой воспроизведения
     * @see PlayProcessor#getTrackChangeHandle()
     */
    public Map<String, String> getState() {
        if(state.isEmpty()) {
            Path a = Path.of(
                    Resources.Properties.DEFAULT_CACHE_TRACKS_PATH.getKey(),
                    FileManager.instance.name(getPlaylistName())
            );

            state.putAll(FileManager.instance.readArray(
                    a.toAbsolutePath().toString(),
                    path,
                    Map.of()
            ));
        }

        return state;
    }

    /**
     * <h3>Время последней остановки как Duration</h3>
     * Загружает позицию остановки из кэша состояния трека.
     * <p>
     * <b>Логика:</b>
     * <ul>
     *   <li><b>Локальный трек:</b> читает {@code TIME} из {@link #getState()}.</li>
     *   <li><b>Сетевой трек:</b> всегда возвращает {@code Duration.ZERO}
     *       (онлайн-треки не кэшируют позицию).</li>
     * </ul>
     * </p>
     *
     * @return Duration для восстановления позиции или 0s для сетевых
     */
    public Duration getRawLastTimeTrack() {
        if(!isNetty) {
            return Duration.seconds(Double.parseDouble(getState().getOrDefault(DataTypes.TIME.code, "0")));
        } else {
            return Duration.seconds(0);
        }
    }

    /**
     * <h3>Ленивая строковая позиция остановки</h3>
     * Кэширует {@link #getRawLastTimeTrack()} в {@link #lastTimeTrack}.
     * <p>
     * <b>Логика:</b>
     * <ul>
     *   <li>{@link #lastTimeTrack} < 0 → загружает и кэширует.</li>
     *   <li>Иначе → возвращает из кэша.</li>
     * </ul>
     * </p>
     *
     * @return секунды как строка
     */
    public String getLastTimeTrack() {
        return lastTimeTrack < 0 ? String.valueOf(lastTimeTrack = (int) getRawLastTimeTrack().toSeconds()) : String.valueOf(lastTimeTrack);
    }

    /**
     * Устанавливает время остановки из строки (Fluent API).
     * <p>
     * Используется при десериализации кэша или ручной установке.
     * </p>
     *
     * @param time время в формате строки
     * @return {@code this}
     */
    public Track setLastTimeTrack(String time) {
        lastTimeTrack = Integer.parseInt(time);
        return this;
    }

    /**
     * Простой геттер общей длительности (без ленивой загрузки).
     * <p>
     * Используется после явной инициализации {@link #totalDuraSec}.
     * </p>
     *
     * @return длительность в секундах (-1 = не загружено)
     */
    public int getTotalDuraSec() {
        return totalDuraSec;
    }

    /**
     * Устанавливает время остановки (Fluent API).
     * <p>
     * Основной метод для сохранения позиции при паузе/стопе.
     * </p>
     *
     * @param lastTimeTrack секунды от начала
     * @return {@code this}
     */
    public Track setLastTimeTrack(int lastTimeTrack) {
        this.lastTimeTrack = lastTimeTrack;
        return this;
    }

    /**
     * <h3>Стандартная миниатюра обложки</h3>
     * Лениво создает мипмап заданного размера {@link ColorProcessor#size}.
     * <p>
     * <b>Логика:</b>
     * <ul>
     *   <li>{@link #mipmap} = null → создает и кэширует с настройками {@link ColorProcessor}.</li>
     *   <li>Иначе → возвращает из кэша.</li>
     * </ul>
     * </p>
     * <p>
     * Используется в ListView для экономии памяти (меньше чем {@link #getAlbumArt()}).
     * </p>
     *
     * @return готовый мипмап стандартного размера
     */
    public Image getMipmap() {
        return mipmap == null ? mipmap = getIndependentAlbumArt(size, size, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth) : mipmap;
    }

    /**
     * Возвращает кэшированное отображаемое имя трека.
     * <p>
     * После вызова {@link #viewName()} содержит {@code "Artist - Title"} или имя файла.
     * </p>
     *
     * @return готовое имя для UI
     */
    public String getViewName() {
        return viewName;
    }

    /**
     * Проверяет статус "фантомного" трека.
     * <p>
     * Фантомные треки автоматически пропускаются в {@link PlayProcessor#next()}.
     * </p>
     *
     * @return {@code true} если трек нужно пропустить
     */
    public boolean isPhantom() {
        return isPhantom;
    }

    /**
     * Помечает трек как фантомный (Fluent API).
     * <p>
     * <b>Применение:</b>
     * <ul>
     *   <li>Поврежденные файлы.</li>
     *   <li>Треки без метаданных.</li>
     *   <li>Временная фильтрация.</li>
     * </ul>
     * </p>
     *
     * @param phantom новый статус
     * @return {@code this}
     */
    public Track setPhantom(boolean phantom) {
        isPhantom = phantom;
        return this;
    }

    /**
     * Возвращает список пользовательских тегов трека.
     * <p>
     * Может быть {@code null} до вызова {@link #setTags(ArrayList)} или {@link #deserializeTags(File)}.
     * </p>
     *
     * @return список тегов или {@code null}
     */
    public ArrayList<Tag> getTags() {
        return tags;
    }

    /**
     * Устанавливает список пользовательских тегов (Fluent API).
     * <p>
     * Заменяет существующие {@link #tags}. Не валидирует содержимое.
     * </p>
     *
     * @param tags новый список тегов
     * @return {@code this} для цепочки вызовов
     */
    public Track setTags(ArrayList<Tag> tags) {
        this.tags = tags;
        return this;
    }

    /**
     * <h3>Сериализация тегов в кэш</h3>
     * Сохраняет {@link #tags} как CSV-строку в файловый кэш.
     * <p>
     * <b>Формат:</b> {@code "rock,90s,favorite,workout"}
     * </p>
     * <p>
     * <b>Путь кэша:</b>
     * <pre>{@code /cache/{filePath}/{trackPath}/track.tags = "rock,90s"}</pre>
     * </p>
     *
     * @param file директория кэша
     * @throws IOException ошибки записи
     * @see FileManager#save(String, String, String, Object)
     */
    public void serializeTags(File file) throws IOException {
        StringBuilder toSave = new StringBuilder();

        for (int i = 0; i < tags.size(); i++) {
            toSave.append(tags.get(i).getName());

            if (i < tags.size() - 1) {
                toSave.append(",");
            }
        }

        FileManager.instance.save(file.getAbsolutePath(), path, "track.tags", toSave.toString());
    }

    /**
     * <h3>Десериализация тегов из кэша</h3>
     * Загружает CSV-строку тегов и парсит в {@link #tags}.
     * <p>
     * <b>Логика:</b>
     * <ol>
     *   <li>Читает {@code track.tags} из кэша.</li>
     *   <li>Инициализирует {@link #tags} если null.</li>
     *   <li>Очищает старые теги.</li>
     *   <li>Парсит CSV → список {@link Tag}.</li>
     * </ol>
     * </p>
     * <p>
     * <b>Пример:</b> {@code "rock,90s"} → {@code [Tag("rock"), Tag("90s")]}
     * </p>
     *
     * @param file директория кэша
     * @throws IOException ошибки чтения
     * @throws ClassNotFoundException внутренние ошибки FileManager
     */
    public void deserializeTags(File file) throws IOException, ClassNotFoundException {
        String buff = FileManager.instance.read(file.getAbsolutePath(), path, "track.tags", "");

        if(tags == null)
            tags = new ArrayList<>();

        tags.clear();

        if(buff != null && !buff.isEmpty()) {
            for (String tag : buff.split(",")) {
                tags.add(new Tag(tag));
            }
        }
    }

    /**
     * Извлекает расширение файла из {@link #toString()} (== {@link #path}).
     * <p>
     * <b>Примеры:</b>
     * <ul>
     *   <li>{@code "/music/song.mp3"} → {@code "mp3"}</li>
     *   <li>{@code "https://song.mp3"} → {@code "mp3"}</li>
     *   *   <li>{@code "song"} → {@code ""}</li>
     * </ul>
     * </p>
     *
     * @return расширение в нижнем регистре или пустая строка
     */
    public String getExtension() {
        String t = toString();

        if(t.contains(".")) {
            return t.substring(t.lastIndexOf(".") + 1);
        } else {
            return "";
        }
    }

    /**
     * <h3>Ленивое определение имени плейлиста</h3>
     * Кэширует название папки/плейлиста в {@link #playlist}.
     * <p>
     * <b>Логика:</b>
     * <ol>
     *   <li>Если {@link #playlist} пустой:
     *     <ul>
     *       <li><b>Сетевой:</b> {@code playlist = path} (URL целиком).</li>
     *       <li><b>Локальный:</b> {@code new File(path).getParentFile().getName()}.</li>
     *     </ul>
     *   </li>
     *   <li>Возвращает кэшированное значение.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Примеры:</b>
     * <pre>
     * "/music/rock/song.mp3" → "rock"
     * "https://song.mp3" → "https://song.mp3"
     * </pre>
     * </p>
     *
     * @return имя папки или URL
     */
    public String getPlaylistName() {
        if(playlist == null || playlist.isEmpty()) {
            if(!isNetty) {
                return playlist = new File(path).getParentFile().getName();
            }
        }

        return playlist;
    }

    /**
     * Устанавливает имя плейлиста вручную (Fluent API).
     * <p>
     * Перезаписывает результат {@link #getPlaylistName()}.
     * </p>
     *
     * @param playlist новое имя плейлиста
     * @return {@code this}
     */
    public Track setPlaylist(String playlist) {
        this.playlist = playlist;
        return this;
    }

    /**
     * Возвращает кэшированное имя плейлиста.
     * <p>
     * Может быть {@code null} до вызова {@link #getPlaylistName()} или {@link #setPlaylist(String)}.
     * </p>
     *
     * @return имя плейлиста или {@code null}
     */
    public String getPlaylist() {
        return playlist;
    }

    /**
     * <h3>Длительность трека</h3>
     * Прямой вызов метаданных без кэширования.
     * <p>
     * В отличие от {@link #getTotalDurationInSeconds()}, всегда читает свежие данные.
     * </p>
     *
     * @return длительность из {@link MetadataOfFile}
     */
    public int getDuration() {
        return MetadataOfFile.iMetadataOfFiles.getDuration(toString());
    }

    /**
     * <h3>Текущее время как Duration</h3>
     * Конвертирует {@link #timeTrack} (секунды) в JavaFX Duration.
     *
     * @return время воспроизведения
     */
    public Duration getTimeTrack() {
        return Duration.seconds(timeTrack);
    }

    /**
     * <h3>Устанавливает текущее время воспроизведения</h3>
     * Обновляет {@link #timeTrack} для синхронизации с плеером.
     *
     * @param timeTrack секунды от начала
     */
    public void setTimeTrack(int timeTrack) {
        this.timeTrack = timeTrack;
    }

    /**
     * <h3>Внешняя ссылка трека</h3>
     * Дополнительный URL (резервный источник, страница альбома).
     *
     * @return внешний URL или {@code null}
     */
    public String getExternalUrl() {
        return externalUrl;
    }

    /**
     * <h3>Ленивая загрузка названия трека</h3>
     * Кэширует результат в {@link #title}.
     * <p>
     * Пустое название остаётся пустым (не fallback на filename).
     * </p>
     *
     * @return название из метаданных
     */
    public String getTitle() {
        return title == null || title.equals("") ? title = MetadataOfFile.iMetadataOfFiles.getTitle(this.path) : title;
    }

    /**
     * <h3>Ленивая загрузка исполнителя</h3>
     * Кэширует результат в {@link #artist}.
     *
     * @return исполнитель из метаданных
     */
    public String getArtist() {
        return artist == null || artist.equals("") ? artist = MetadataOfFile.iMetadataOfFiles.getArtist(this.path) : artist;
    }

    /**
     * <h3>Устанавливает общую длительность</h3>
     * Перезаписывает кэш {@link #totalDuraSec} (Fluent API).
     *
     * @param totalDuraSec секунды
     * @return {@code this}
     */
    public Track setTotalDuraSec(int totalDuraSec) {
        this.totalDuraSec = totalDuraSec;
        return this;
    }

    /**
     * <h3>Устанавливает внешнюю ссылку</h3>
     * Для резервных источников или метаданных (Fluent API).
     *
     * @param externalUrl новый URL
     * @return {@code this}
     */
    public Track setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
        return this;
    }

    /**
     * <h3>Имя файла без расширения</h3>
     * Усекает имя файла до первого вхождения разделителя.
     * <p>
     * <b>Пример:</b> {@code "Artist - Song.mp3"} + {@code "."} → {@code "Artist - Song"}
     * </p>
     *
     * @param splitter разделитель (обычно {@code "."})
     * @return имя без хвоста
     */
    public String getName(String splitter) {
        String name = new File(path).getName();
        return name.substring(0, name.indexOf(splitter));
    }

    /**
     * <h3>Полное имя файла</h3>
     * Возвращает basename из {@link #path}.
     * <p>
     * <b>Примеры:</b>
     * <ul>
     *   <li>{@code "/music/song.mp3"} → {@code "song.mp3"}</li>
     *   <li>{@code "https://song.mp3"} → {@code "song.mp3"}
     * </ul>
     * </p>
     *
     * @return имя файла с расширением
     */
    public String getName() {
        return new File(path).getName();
    }

    /**
     * <h3>Устанавливает название трека</h3>
     * Перезаписывает кэш {@link #title} (Fluent API).
     * <p>
     * Обходит {@link #getTitle()} ленивую загрузку из метаданных.
     * </p>
     *
     * @param title новое название
     * @return {@code this}
     */
    public Track setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * <h3>Устанавливает исполнителя</h3>
     * Перезаписывает кэш {@link #artist} (Fluent API).
     *
     * @param artist новый исполнитель
     * @return {@code this}
     */
    public Track setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    /**
     * <h3>Устанавливает отображаемое имя</h3>
     * Перезаписывает результат {@link #viewName()} (Fluent API).
     *
     * @param viewName готовое {@code "Artist - Title"}
     * @return {@code this}
     */
    public Track setViewName(String viewName) {
        this.viewName = viewName;
        return this;
    }

    /**
     * <h3>Path-версия ссылки</h3>
     * Конвертирует {@link #path} String → {@link Path}.
     *
     * @return Path для NIO операций
     */
    public Path getFilePath() {
        return Paths.get(path);
    }

    /**
     * <h3>Изменяет путь трека</h3>
     * Полная перезагрузка идентификатора (Fluent API).
     * <p>
     * <b>Сбрасывает:</b> кэш метаданных, плейлист, состояние.
     * </p>
     *
     * @param path новый путь/URL
     * @return {@code this}
     */
    public Track setPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * <h3>Восстанавливать ли позицию воспроизведения</h3>
     * Читает настройку {@link #restoreState}.
     *
     * @return {@code true} = прыгать на {@link #lastTimeTrack}
     */
    public boolean isRestoreState() {
        return restoreState;
    }

    /**
     * <h3>Включает/выключает восстановление позиции</h3>
     * Настройка поведения {@link PlayProcessor#open(Track)} (Fluent API).
     *
     * @param restoreState новый режим
     * @return {@code this}
     */
    public Track setRestoreState(boolean restoreState) {
        this.restoreState = restoreState;
        return this;
    }

    /**
     * <h3>Основной идентификатор трека</h3>
     * Универсальная ссылка — путь к файлу или URL.
     * <p>
     * Используется везде: {@link Track#equals(Object)} ()}, кэширование, {@link MediaPlayer}.
     * </p>
     *
     * @return {@link #path} (никогда не {@code null} после конструктора)
     */
    public String getPath() {
        return path;
    }

    /**
     * <h3>Форматирует длительность → читаемый вид</h3>
     * Конвертирует секунды в {@code "3:45"} или {@code "1:02:30"}.
     * <p>
     * <b>Логика:</b>
     * <ul>
     *   <li>≥ 1ч: {@code "H:MM:SS"} (1:02:30)</li>
     *   <li>< 1ч:  {@code "MM:SS"} (3:45)</li>
     * </ul>
     * </p>
     *
     * @param length секунды (float)
     * @return отформатированная строка
     */
    public static String getFormattedTotalDuration(float length) {
        int totalSeconds = Math.round(length);

        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    /**
     * Парсит строку длительности трека в секунды.
     * <p>
     * <b>Поддерживаемый формат:</b> {@code "MM:SS"} (например, "3:45", "12:05").
     * </p>
     *
     * <h4>Логика парсинга (пошагово):</h4>
     * <ol>
     *   <li>{@code split(":")} → {@code ["3", "45"]}</li>
     *   <li>{@code parseInt(parts[0])} → {@code minutes = 3}</li>
     *   <li>{@code parseInt(parts[1])} → {@code seconds = 45}</li>
     *   <li>{@code minutes * 60 + seconds} → {@code 3 * 60 + 45 = 225 секунд}</li>
     * </ol>
     *
     * @param time строка длительности в формате "MM:SS"
     * @return общее время в секундах
     * @throws ArrayIndexOutOfBoundsException если формат != "MM:SS" (нет ":")
     * @throws NumberFormatException если минуты/секунды не числа
     */
    public static int getFormattedTotalDuration(String time) {
        // Разбиение строки по разделителю ":"
        String[] parts = time.split(":");

        // Парсинг минут (первая часть до ":")
        int minutes = Integer.parseInt(parts[0].trim());
        // Парсинг секунд (вторая часть после ":")
        int seconds = Integer.parseInt(parts[1].trim());

        // Конвертация в общее количество секунд
        return minutes * 60 + seconds;
    }

    /**
     * Асинхронно парсит ссылку на скачивание трека через всех доступных провайдеров.
     * <p>
     * <b>Детальная логика работы (пошагово):</b>
     * </p>
     *
     * <h4>1. Однопоточный исполнитель</h4>
     * <pre>ExecutorService executor = newSingleThreadExecutor()</pre>
     * Создаётся <b>один поток</b> для последовательного выполнения провайдеров.
     *
     * <h4>2. Создание задач для каждого провайдера</h4>
     * <pre>for (Info.IInfo a : playersMap.values())</pre>
     * Для каждого провайдера из {@code playersMap} создаётся задача:
     * <pre>{@code () -> a.getTrackDownloadLink(track)}</pre>
     *
     * <h4>3. invokeAny() - "первый успех выигрывает"</h4>
     * <pre>executor.invokeAny(tasks)</pre>
     * <b>КРИТИЧНО:</b> Выполняет задачи <b> последовательно </b> до первого успеха:
     * <ul>
     * <li>Провайдер 1 (Hitmos): успех → <b>НЕМЕДЛЕННЫЙ ВОЗВРАТ</b></li>
     * <li>Провайдер 2 (Musmore): не вызывается (уже вернули Hitmos)</li>
     * </ul>
     *
     * <h4>4. Валидация результата</h4>
     * <pre>if (url == null) throw new IOException("Invalid URL")</pre>
     * Каждый провайдер обязан вернуть валидный Track или выбросить исключение.
     *
     * <h4>5. Обработка ошибок</h4>
     * <ul>
     * <li><b>ExecutionException</b> - любой провайдер выбросил исключение</li>
     * <li><b>InterruptedException</b> - основной поток прерван</li>
     * </ul>
     * Оба пробрасываются вызывающему коду.
     *
     * <h3>Пример сценария</h3>
     * <pre>{@code
     * playersMap = [Hitmos, Musmore, LightAudio]
     * track = "The Weeknd - Blinding Lights"
     *
     * 1. Hitmos.getTrackDownloadLink() = ✓ "https://..." (0.3с)
     *    → ВОЗВРАЩАЕТСЯ НЕМЕДЛЕННО, Musmore/LightAudio игнорируются
     *
     * 2. Hitmos.getTrackDownloadLink() = ✗ null
     *    → Musmore.getTrackDownloadLink() = ✓ "https://..." (0.8с)
     * }</pre>
     *
     * <p><b>Производительность:</b> Останавливается на первом успехе, не дожидается остальных.</p>
     *
     * @param track название/идентификатор трека для поиска
     * @return Track с валидной ссылкой на скачивание от первого успешного провайдера
     * @throws ExecutionException если все провайдеры вернули null/исключение
     * @throws InterruptedException если поток прерван
     * @see Info#playersMap
     * @since 0.1.4
     */
    public static Track parseTrackFromNetworkAsync(String track) throws ExecutionException, InterruptedException {
        // Создание однопоточного ExecutorService с авто-закрытием (try-with-resources)
        // try-with-resources гарантирует полное закрытие executor даже при исключениях
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            try {
                // Создание списка задач для каждого провайдера из playersMap
                List<Callable<Track>> tasks = new ArrayList<>();

                // Формирование задач: для каждого провайдера создаем lambda
                for (Info.IInfo a : playersMap.values()) {
                    tasks.add(() -> {
                        // Вызов провайдера для получения ссылки на скачивание
                        Track url = a.getTrackDownloadLink(track);

                        // Валидация: провайдер обязан вернуть не-null Track
                        if (url == null) {
                            throw new IOException("Invalid URL");
                        }

                        // Возврат успешного результата
                        return url;
                    });
                }

                // invokeAny(): последовательно выполняет задачи до ПЕРВОГО успеха
                // Остальные задачи отменяются автоматически при первом успехе
                return executor.invokeAny(tasks);
            } finally {
                // Принудительное завершение executor (на случай invokeAny не завершился)
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
                    SwingFXUtils.fromFXImage(Root.artProcessor.parseImage(newValue.viewName()), null));
        }
    }

    public static URL getURIFromTrack(rf.ebanina.ebanina.Player.Track newValue) {
        try {
            String urlString = newValue.toString();

            if (urlString == null || urlString.equalsIgnoreCase(Info.PlayersTypes.URI_NULL.getCode())) {

                urlString = Objects.requireNonNull(Track.parseTrackFromNetworkAsync(newValue.viewName())).getPath();

                if (urlString == null) {
                    Music.mainLogger.println(Prefix.ERROR, "URL для загрузки отсутствует");

                    return null;
                }
            }

            return new URL(urlString);
        } catch (IOException e) {
            Music.mainLogger.err(e);

            return null;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <h3>Форматирует double-длительность</h3>
     * Делегирует {@link #getFormattedTotalDuration(float)}.
     */
    public static String getFormattedTotalDuration(double length) {
        return getFormattedTotalDuration((float) length);
    }

    /**
     * <h3>Форматирует int-длительность</h3>
     * Делегирует {@link #getFormattedTotalDuration(float)}.
     */
    public static String getFormattedTotalDuration(int length) {
        return getFormattedTotalDuration((float) length);
    }

    /**
     * <h3>Форматирует длительность текущего трека</h3>
     * {@link #getTotalDurationInSeconds()} → {@code "3:45"}.
     */
    public String getFormattedTotalDuration() {
        return getFormattedTotalDuration(getTotalDurationInSeconds());
    }

    /**
     * <h3>Сериализует трек в файл</h3>
     * Полное сохранение объекта через {@link FileManager}.
     *
     * @param filePath путь к файлу
     */
    public void write(String filePath) {
        FileManager.instance.writeObject(this, filePath);
    }

    /**
     * <h3>Десериализует трек из файла</h3>
     * Восстанавливает объект через {@link FileManager}.
     *
     * @param filePath путь к файлу
     * @return восстановленный {@link Track}
     */
    public Object read(String filePath) {
        return FileManager.instance.readObject(filePath);
    }

    /**
     * <h3>Глубокое клонирование трека</h3>
     * Создаёт полную копию объекта через стандартный механизм Java.
     * <p>
     * Клонируются все поля, включая кэшированные метаданные и обложки.
     * </p>
     *
     * @return независимая копия {@link Track}
     * @throws CloneNotSupportedException если класс не поддерживает клонирование
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * <h3>Равенство по пути</h3>
     * Два трека равны, если имеют одинаковый {@link #path}.
     * <p>
     * <b>Проверки:</b>
     * <ul>
     *   <li>Сначала {@code this == o} (идентичность).</li>
     *   <li>Затем тип и {@code path.equals()} (с null-check).</li>
     * </ul>
     * </p>
     * <p>
     * Гарантирует корректную работу в {@link Set}, {@link Map}.
     * </p>
     *
     * @param o объект для сравнения
     * @return {@code true} при равенстве путей
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Track track = (Track) o;

        if(path == null)
            return false;

        if(track.getPath() == null)
            return false;

        return path.equals(track.getPath());
    }

    /**
     * <h3>HashCode по пути</h3>
     * Консистентен с {@link #equals(Object)}.
     *
     * @return hash {@link #path}
     */
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    /**
     * <h3>Возвращает путь</h3>
     * <p>
     * Сейчас используется как {@link #path}
     * </p>
     *
     * @return {@link #path} напрямую
     */
    @Override
    public String toString() {
        return path;
    }

    /**
     * <h3>Сортировка по отображаемому имени</h3>
     * Лексикографическое сравнение {@link #viewName()}.
     * <p>
     * <b>Порядок:</b> "Artist - Title" (A→Z).
     * </p>
     *
     * @param o трек для сравнения
     * @return отрицательное/нулевое/положительное (стандартный compareTo)
     */
    @Override
    public int compareTo(Track o) {
        return viewName().compareTo(o.viewName());
    }
}