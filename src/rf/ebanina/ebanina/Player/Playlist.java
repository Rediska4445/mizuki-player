package rf.ebanina.ebanina.Player;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;

/**
 * <h1>Playlist</h1>
 * Класс, представляющий плейлист треков.
 * <p>
 * {@code Playlist} инкапсулирует путь к файлу плейлиста, его отображаемое имя и логическое имя.
 * Позволяет удобно хранить и получать характеристики плейлиста для дальнейшей работы в плеере.
 * </p>
 * <p>
 * В будущих версиях планируется расширение функционала для поддержки сетевых плейлистов, что позволит
 * работать с удалёнными ресурсами и потоками.
 * </p>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * // Создание плейлиста с указанием пути
 * Playlist playlist = new Playlist("/music/playlist.m3u");
 *
 * // Установка отображаемого имени
 * playlist.setViewName("Мой плейлист");
 *
 * // Получение имени файла плейлиста
 * String fileName = playlist.getFileName();
 *
 * // Получение имени с возможностью динамического определения
 * String name = playlist.getName();
 * }</pre>
 *
 * <h2>Особенности реализации</h2>
 * <ul>
 *   <li>Метод {@link #getName()} возвращает заданное имя, либо, если оно отсутствует, вычисляет имя файла из пути.</li>
 *   <li>Переопределены методы {@link #equals(Object)} и {@link #hashCode()} по полю {@code path}, что позволяет использовать {@code Playlist} в коллекциях.</li>
 *   <li>Метод {@link #toString()} возвращает путь плейлиста — удобен для отладки и логирования.</li>
 * </ul>
 *
 * @author Ebanina Std.
 * @version 1.4.1
 * @since 1.4.0
 */
public class Playlist
        implements Serializable, Cloneable, MediaReference, Comparable<Playlist>
{
    private String path;
    private String viewName;
    private String name;

    private boolean isNetty;

    public Playlist() {
        this("");
    }

    public Playlist(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public Playlist(String path, String viewName, String name) {
        this.path = path;
        this.viewName = viewName;
        this.name = name;
    }

    public Playlist(String path) {
        this.path = path;
    }

    public Playlist setNetty(boolean netty) {
        isNetty = netty;
        return this;
    }

    /**
     * Возвращает отображаемое имя плейлиста, используемое в интерфейсе.
     *
     * @return отображаемое имя или {@code null}, если не установлено
     */
    public String getViewName() {
        return viewName;
    }

    /**
     * Устанавливает отображаемое имя плейлиста.
     *
     * @param viewName отображаемое имя для плейлиста
     * @return текущий экземпляр {@code Playlist} для цепочных вызовов
     */
    public Playlist setViewName(String viewName) {
        this.viewName = viewName;
        return this;
    }

    /**
     * Возвращает путь к файлу плейлиста.
     *
     * @return строка с путём к файлу
     */
    public String getPath() {
        return path;
    }

    /**
     * Устанавливает путь к файлу плейлиста.
     *
     * @param path путь к файлу
     * @return текущий экземпляр {@code Playlist} для цепочных вызовов
     */
    public Playlist setPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Возвращает имя плейлиста.
     * <p>Если имя ещё не установлено, автоматически вычисляет его как имя файла по пути.</p>
     *
     * @return имя плейлиста
     */
    public String getName() {
        return name == null ? name = getFileName() : name;
    }

    /**
     * Возвращает имя файла файла плейлиста, извлечённое из пути.
     *
     * @return имя файла с расширением
     */
    public String getFileName() {
        return Path.of(path).getFileName().toString();
    }

    /**
     * Устанавливает имя плейлиста.
     *
     * @param name имя плейлиста
     * @return текущий экземпляр {@code Playlist} для цепочных вызовов
     */
    public Playlist setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Проверяет равенство по пути плейлиста.
     *
     * @param o другой объект для сравнения
     * @return {@code true}, если пути совпадают, иначе {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Playlist playlist = (Playlist) o;
        return Objects.equals(path, playlist.path);
    }

    /**
     * Вычисляет хеш-код по пути плейлиста.
     *
     * @return целочисленное значение хеш-кода
     */
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    /**
     * Возвращает строковое представление плейлиста, по умолчанию путь к файлу.
     *
     * @return путь к файлу плейлиста
     */
    @Override
    public String toString() {
        return getPath();
    }

    @Override
    public int compareTo(Playlist o) {
        return path.compareTo(o.getPath());
    }

    @Override
    public boolean isNetty() {
        return isNetty;
    }
}
