package ebanina.media;

import javafx.scene.image.Image;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rf.ebanina.ebanina.Player.Track;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class TrackTest
        extends ebanina.Test
{
    @BeforeAll
    static void initJfx() {

    }

    @Test
    void testEmptyConstructor() {
        // When: создаём пустой трек
        Track track = new Track();

        // Then: все поля должны быть в начальном состоянии
        assertNull(track.getPath(), "Path должен быть null");
        assertFalse(track.isNetty(), "isNetty должен быть false");
        assertFalse(track.isPhantom(), "isPhantom должен быть false");
        assertNull(track.getTitle(), "title должен быть null");
        assertNull(track.getArtist(), "artist должен быть null");
        assertNull(track.getViewName(), "viewName должен быть null");

        assertEquals(-1, track.totalDuraSec, "totalDuraSec должен быть -1");
        assertEquals(-1, track.lastTimeTrack, "lastTimeTrack должен быть -1");

        assertNull(track.albumArt, "albumArt должен быть null");
        assertNull(track.mipmap, "mipmap должен быть null");
        assertNull(track.getTags(), "tags должен быть null");
    }

    @Test
    void testLocalTrackConstructor() throws MalformedURLException {
        // Given: реальный путь к mp3 файлу из ресурсов
        String mp3Path = String.valueOf(guineaPigs("metadata" + File.separator + "mp.mp3"));
        Track track = new Track(mp3Path);

        // Then: корректная инициализация локального трека
        assertEquals(mp3Path, track.getPath(), "Path должен совпадать");
        assertFalse(track.isNetty(), "Локальный трек не должен быть сетевым");
        assertFalse(track.isPhantom(), "isPhantom должен быть false по умолчанию");

        // Проверяем вычисляемые поля
        assertEquals("mp.mp3", track.getName(), "Имя файла");
        assertEquals("mp3", track.getExtension(), "Расширение mp3");
        String expectedPlaylist = new File(mp3Path).getParentFile().getName();
        assertEquals(expectedPlaylist, track.getPlaylistName(), "Имя плейлиста из parent");
    }

    @Test
    void testLocalTrackWavConstructor() throws MalformedURLException {
        // Given: wav файл для проверки универсальности
        String wavPath = String.valueOf(guineaPigs("metadata" + File.separator + "wav.wav"));
        Track track = new Track(wavPath);

        // Then: работает с любыми аудио форматами
        assertEquals(wavPath, track.getPath());
        assertFalse(track.isNetty());
        assertEquals("wav.wav", track.getName());
        assertEquals("wav", track.getExtension());
    }

    @Test
    void testOnlineTrackConstructor() throws MalformedURLException {
        // Given: URL онлайн трека
        URL url = new URL("https://example.com/song.mp3");
        Track track = new Track(url);

        // Then: корректная инициализация сетевого трека
        assertEquals("https://example.com/song.mp3", track.getPath(), "URL как строка");
        assertTrue(track.isNetty(), "Сетевой трек должен иметь isNetty = true");
        assertFalse(track.isPhantom());

        // Проверяем особые случаи для сетевых треков
        assertNull(track.getPlaylistName(), "Для сетевых playlistName = null URL");
        assertEquals("mp3", track.getExtension());
        assertEquals("song.mp3", track.getName());
    }

    @Test
    void testConstructorEquals() throws MalformedURLException {
        // Given: два трека с одинаковым путём
        String path = String.valueOf(guineaPigs("metadata" + File.separator + "mp.mp3"));
        Track track1 = new Track(path);
        Track track2 = new Track(path);

        // Then: равенство работает правильно
        assertEquals(track1, track2, "Треки с одинаковым путём равны");
        assertEquals(track1.hashCode(), track2.hashCode(), "Одинаковый hashCode");

        // When: разные пути
        Track track3 = new Track("https://example.com/other.mp3");
        assertNotEquals(track1, track3, "Разные пути = разные треки");
    }

    @Test
    void testConstructorStateInitialization() {
        // Given: трек из конструктора
        String path = String.valueOf(guineaPigs("metadata" + File.separator + "mp.mp3"));
        Track track = new Track(path);

        // Then: состояние воспроизведения в дефолтных значениях
        assertEquals(-1, track.lastTimeTrack);
        assertEquals(-1, track.totalDuraSec);
    }

    @Test
    void testPathMethods() throws MalformedURLException {
        String mp3Path = String.valueOf(guineaPigs("metadata" + File.separator + "mp.mp3"));
        Track track = new Track(mp3Path);

        assertEquals(mp3Path, track.getPath(), "getPath возвращает установленный путь");
        assertEquals("mp.mp3", track.getName(), "getName возвращает имя файла с расширением");
        assertEquals("mp", track.getName("."), "getName('.') убирает расширение");
        assertEquals("mp3", track.getExtension(), "getExtension извлекает mp3");

        Path expectedPath = Path.of(mp3Path);
        assertEquals(expectedPath, track.getFilePath(), "getFilePath конвертирует в Path");
        assertEquals(mp3Path, track.getFilePath().toString(), "Path.toString() совпадает с getPath()");
    }

    /**
     * Тестирует ленивую загрузку и кэширование getPlaylistName()
     */
    @Test
    void testPlaylistNameLazyLoading() throws MalformedURLException {
        String mp3Path = String.valueOf(guineaPigs("metadata" + File.separator + "mp.mp3"));
        Track track = new Track(mp3Path);

        File file = new File(mp3Path);
        String expectedPlaylist = file.getParentFile().getName();
        assertEquals(expectedPlaylist, track.getPlaylistName(), "playlistName из parent папки");

        String firstCall = track.getPlaylistName();
        String secondCall = track.getPlaylistName();
        assertSame(firstCall, secondCall, "getPlaylistName кэшируется");
    }

    /**
     * Тестирует setPath() - полное обновление всех вычисляемых полей
     */
    @Test
    void testSetPathChangesEverything() throws MalformedURLException {
        String oldPath = String.valueOf(guineaPigs("metadata" + File.separator + "mp.mp3"));
        String newPath = String.valueOf(guineaPigs("metadata" + File.separator + "wav.wav"));

        Track track = new Track(oldPath);
        track.setPath(newPath);

        assertEquals(newPath, track.getPath(), "setPath обновляет основной путь");
        assertEquals("wav.wav", track.getName(), "getName обновилось");
        assertEquals("wav", track.getExtension(), "getExtension обновилось");
        assertEquals(new File(newPath).getParentFile().getName(), track.getPlaylistName());
    }

    /**
     * Тестирует ручное управление плейлистом через getPlaylist()/setPlaylist()
     */
    @Test
    void testPlaylistManualControl() throws MalformedURLException {
        String path = String.valueOf(guineaPigs("metadata" + File.separator + "mp.mp3"));
        Track track = new Track(path);

        String customPlaylist = "MyFavorites";
        assertNull(track.getPlaylist(), "getPlaylist() изначально null");

        track.setPlaylist(customPlaylist);
        assertEquals(customPlaylist, track.getPlaylist(), "setPlaylist сохраняется");
        assertEquals(customPlaylist, track.getPlaylistName(), "getPlaylistName использует ручное значение");

        track.setPlaylist(null);
        assertNull(track.getPlaylist(), "setPlaylist(null) сбрасывает");
        assertEquals(new File(path).getParentFile().getName(), track.getPlaylistName(), "возврат к автоопределению");
    }

    /**
     * Тестирует граничные случаи работы с путями
     */
    @Test
    void testPathEdgeCases() throws MalformedURLException {
        Track track;

        track = new Track("/music/song");
        assertEquals("song", track.getName());
        assertEquals("", track.getExtension(), "нет точки = пустое расширение");

        track = new Track("song.mp3");
        assertEquals("song.mp3", track.getName());
        assertEquals("mp3", track.getExtension());
        Track finalTrack = track;
        assertThrows(NullPointerException.class, () -> finalTrack.getPlaylistName(), "без parent = исключение");

        track = new Track(new URL("https://sub.domain.com/music/song.mp3"));
        assertNull(track.getPlaylistName(), "сетевой = полный URL");
        assertEquals("mp3", track.getExtension());
    }

    /**
     * Тестирует связь getFilePath().getParent() и getPlaylistName()
     */
    @Test
    void testFilePathParentRelationship() throws MalformedURLException {
        String fullPath = String.valueOf(guineaPigs("metadata" + File.separator + "mp.mp3"));
        Track track = new Track(fullPath);

        Path filePath = track.getFilePath();
        String pathParentName = filePath.getParent().toFile().getName();
        assertEquals(pathParentName, track.getPlaylistName(), "getFilePath().getParent() == getPlaylistName()");
    }
    /**
     * Тестирует кэширование длительности в getTotalDurationInSeconds()
     */
    @Test
    void testTotalDurationInSecondsCaching() {
        String mp3Path = guineaPigs("metadata" + File.separator + "mp.mp3").toString();
        Track track = new Track(mp3Path);

        // Первый вызов загружает из метаданных
        int firstDuration = track.getTotalDurationInSeconds();
        assertTrue(firstDuration > 0, "Длительность должна быть положительной");

        // Второй вызов из кэша (тот же результат)
        int secondDuration = track.getTotalDurationInSeconds();
        assertEquals(firstDuration, secondDuration, "Кэширование работает");
    }

    /**
     * Тестирует простой геттер и установку длительности
     */
    @Test
    void testTotalDuraSecGetterSetter() {
        Track track = new Track("test.mp3");

        assertEquals(-1, track.getTotalDuraSec(), "Изначально -1");

        track.setTotalDuraSec(225);
        assertEquals(225, track.getTotalDuraSec(), "setTotalDuraSec сохраняет значение");
    }

    /**
     * Тестирует все перегрузки форматирования длительности
     */
    @Test
    void testFormattedTotalDurationAllOverloads() {
        // float → String
        assertEquals("3:45", Track.getFormattedTotalDuration(225.0f), "float overload");
        assertEquals("3:46", Track.getFormattedTotalDuration(225.7f), "округление работает");

        // double → String
        assertEquals("3:45", Track.getFormattedTotalDuration(225.0), "double overload");

        // int → String
        assertEquals("3:45", Track.getFormattedTotalDuration(225), "int overload");

        // String → int (парсинг)
        assertEquals(225, Track.getFormattedTotalDuration("3:45"), "MM:SS парсинг");
        assertEquals(3903, Track.getFormattedTotalDuration("65:03"), "длинные минуты");

        // часы
        assertEquals("1:02:30", Track.getFormattedTotalDuration(3750f), "H:MM:SS формат");
    }

    /**
     * Тестирует текущее время воспроизведения
     */
    @Test
    void testTimeTrackMethods() {
        Track track = new Track(String.valueOf(guineaPigs("metadata" + File.separator + "mp.mp3")));

        assertEquals(0, track.getTimeTrack().toSeconds(), "Изначально 0");

        track.setTimeTrack(120);
        assertEquals(120, track.getTimeTrack().toSeconds(), "setTimeTrack → Duration");
    }

    /**
     * Тестирует кэширование времени остановки
     */
    @Test
    void testLastTimeTrackCaching() {
        Track track = new Track(String.valueOf(guineaPigs("metadata" + File.separator + "mp.mp3")));

        // Кэш будет тестироваться отдельно
        track.setLastTimeTrack("0");

        // Изначально из состояния (0)
        assertEquals("0", track.getLastTimeTrack(), "Кэш пустой → состояние");

        // После установки кэшируется
        track.setLastTimeTrack(45);
        assertEquals("45", track.getLastTimeTrack(), "Кэш работает");
    }

    /**
     * Тестирует время остановки для сетевых треков (всегда 0)
     */
    @Test
    void testRawLastTimeTrackNettyAlwaysZero() throws MalformedURLException {
        Track online = new Track(new URL("https://example.com/song.mp3"));

        assertEquals(Duration.ZERO, online.getRawLastTimeTrack(), "Сетевые треки = 0");
    }

    /**
     * Тестирует обе перегрузки setLastTimeTrack
     */
    @Test
    void testSetLastTimeTrackOverloads() {
        Track track = new Track(String.valueOf(guineaPigs("metadata" + File.separator + "mp.mp3")));

        // int версия
        track.setLastTimeTrack(120);
        assertEquals("120", track.getLastTimeTrack());

        // String версия
        track.setLastTimeTrack("90");
        assertEquals("90", track.getLastTimeTrack());
    }

    /**
     * Тестирует все перегрузки getAlbumArt()
     */
    @Test
    void testAlbumArtOverloads() {
        Track track = new Track(String.valueOf(guineaPigs("metadata" + File.separator + "mp.mp3")));

        // Без параметров (стандартный размер)
        Image defaultArt = track.getAlbumArt();

        // С одним параметром (квадрат)
        Image squareArt = track.getAlbumArt(100);

        // Полные параметры
        Image customArt = track.getAlbumArt(200, 150, true, true);

        // Кэширование
        assertSame(track.getAlbumArt(), defaultArt, "getAlbumArt() кэшируется");
    }

    /**
     * Тестирует мипмапы и независимые обложки
     */
    @Test
    void testMipmapAndIndependentArt() {
        Track track = new Track(String.valueOf(guineaPigs("metadata" + File.separator + "mp.mp3")));

        // Стандартный мипмап
        Image mipmap = track.getMipmap();

        // Минимальный мипмап
        Image smallMipmap = track.getMipmap(32);

        // Независимая обложка (НЕ кэшируется)
        Image independent1 = track.getIndependentAlbumArt(100, 100, true, true);
        Image independent2 = track.getIndependentAlbumArt(100, 100, true, true);
        assertNotSame(independent1, independent2, "getIndependentAlbumArt не кэширует");
    }
}
