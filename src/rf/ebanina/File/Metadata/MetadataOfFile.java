package rf.ebanina.File.Metadata;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Metadata.Formats.IFormatAudioMetadata;
import rf.ebanina.File.Metadata.Formats.MP3;
import rf.ebanina.File.Metadata.Formats.WAV;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static rf.ebanina.Network.Info.PlayersTypes.URI_NULL;

/**
 * Класс, представляющий метаданные аудиофайлов различных форматов.
 * <p>
 * {@code MetadataOfFile} является фасадом для работы с метаданными аудиофайлов,
 * поддерживая несколько форматов (например, MP3 и WAV) через карту обработчиков {@link #metadataFormatsMap}.
 * Это позволяет централизованно получать и изменять сведения о треках, такие как названия, исполнители,
 * длительность и обложки.
 * </p>
 * <p>
 * Класс реализует интерфейс {@link IAudioMetadataOfFile} и делегирует операции конкретным
 * форматам, определяемым по расширению файла. Если формат не поддерживается,
 * возвращаются значения по умолчанию (например, "Unknown Title").
 * </p>
 * <p>
 * Особенности реализации:
 * <ul>
 *   <li>Использование карты {@link #metadataFormatsMap} для хранения экземпляров классов обработки форматов.</li>
 *   <li>Обработка получения обложки из файла и последующая кэшировка при необходимости.</li>
 *   <li>Методы получения и модификации метаданных универсальны и основаны на расширении файла.</li>
 *   <li>Поддерживаются дополнительные методы для получения всех ключей и значений метаданных.</li>
 * </ul>
 * </p>
 * <p>
 * Предполагается, что работа с физическими файлами и сетевыми ресурсами уже осуществляется
 * в соответствующих классах форматов (например, {@link MP3}, {@link WAV}).
 * </p>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * MetadataOfFile metadata = new MetadataOfFile();
 * String title = metadata.getTitle("/music/song.mp3");
 * metadata.setArtist("/music/song.mp3", "New Artist");
 * Image albumArt = metadata.getArt("/music/song.mp3", 128, 128, true, true);
 * }</pre>
 *
 * <h2>Изначально поддерживаемые форматы</h2>
 * <ul>
 *   <li>MP3</li>
 *   <li>WAV</li>
 * </ul>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li>Если расширение файла не поддерживается, возвращаются значения по умолчанию или пустые коллекции.</li>
 *   <li>Методы работы с длительностью зависят от внешних ресурсов (например, плеера {@code mediaPlayer}).</li>
 * </ul>
 *
 * @author Ebanina Std.
 * @version 0.1.4.4
 * @since 0.1.3
 * @see IAudioMetadataOfFile
 * @see MP3
 * @see WAV
 * @see javafx.scene.image.Image
 */
public final class MetadataOfFile
        implements IAudioMetadataOfFile
{
    /**
     * Общедоступный, статический, не константный единый экземпляр класса {@link MetadataOfFile}.
     * <p>
     * Этот экземпляр существует как единственный возможный путь для работы с метаданными аудио.
     * {@code public static MetadataOfFile iMetadataOfFiles} создаётся с уже добавленной картой с {@link MP3} и {@link WAV}
     * </p>
     * <p>
     * Экземпляр можно переустановить, так как он не финальный.
     * Для расширения форматов, можно вызвать {@link Map#put} у карты {@link MetadataOfFile#metadataFormatsMap} соответственно.
     * </p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * String title = iMetadataOfFiles.getTitle("path.wav");
     * if (title != null) {
     *     System.out.println(title);
     * }
     * }</pre>
     *
     * @version 0.1.4.4
     */
    public static MetadataOfFile iMetadataOfFiles = new MetadataOfFile(new HashMap<>(Map.of(
            "mp3", new MP3(),
            "wav", new WAV()
    )));
    /**
     * Карта форматов, сопоставляющая расширения аудиофайлов с объектами для работы с метаданными.
     * <p>
     * Это, общедоступное поле с неизменной ссылкой, но внутренне содержащая
     * изменяемый {@link HashMap}. Изначально содержит пары "mp3" → {@code mp3Format} и "wav" → {@code wavFormat}.
     * </p>
     * <p>
     * Использование мапы позволяет обращать методы к конкретному обработчику формата
     * в зависимости от расширения файла.
     * </p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * IFormatAudioMetadata format = metadataFormatsMap.get("mp3");
     * if (format != null) {
     *     String title = format.getTitle("/music/song.mp3");
     * }
     * }</pre>
     *
     * @version 0.1.4.4
     */
    public Map<String, IFormatAudioMetadata> metadataFormatsMap;
    /**
     * Значение по умолчанию для названия трека, возвращаемое при отсутствии данных.
     * <p>
     * Интерфейс предназначен для предоставления осмысленного результата вместо {@code null}
     * или пустой строки, когда метаданные отсутствуют или не опознаны.
     * </p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * String title = metadata.getTitle("/unknown/file.xyz");
     * // Если формат не поддерживается, вернется unkTitle
     * }</pre>
     *
     * @version 0.1.4.3
     */
    public String unkTitle = "Unknown Title";
    /**
     * Значение по умолчанию для имени исполнителя, возвращаемое при отсутствии данных.
     * <p>
     * Используется для замены пустых или нераспознанных значений исполнителя,
     * обеспечивая консистентность данных.
     * </p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * String artist = metadata.getArtist("/unknown/file.xyz");
     * // Если исполнитель не найден, вернется unkAuthor
     * }</pre>
     *
     * @version 0.1.4.3
     */
    public String unkAuthor = "Unknown Author";

    public MetadataOfFile(Map<String, IFormatAudioMetadata> metadataFormatsMap) {
        this.metadataFormatsMap = metadataFormatsMap;
    }

    /**
     * Метод возвращает длительность текущего трека в секундах.
     * <p>
     * <h2>Реализация</h2>
     * В данной реализации вызывается метод {@code getOverDuration()} объекта {@code mediaPlayer}
     * и результат конвертируется в целочисленный тип для совместимости с интерфейсом.
     * </p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * int durationSeconds = getDuration("/music/track.mp3");
     * System.out.println("Длительность: " + durationSeconds + " секунд");
     * }</pre>
     *
     * @param path путь к аудиофайлу для возможной обработки расширения или других целей
     * @return длительность в секундах
     * @version 0.1.0
     */
    @Override
    public int getDuration(String path) {
        return (int) MediaProcessor.mediaProcessor.mediaPlayer.getOverDuration().toSeconds();
    }
    /**
     * Метод возвращает название трека из метаданных файла.
     * <p>
     * <h2>Реализация</h2>
     * Он ищет обработчик формата по расширению файла, полученному из {@code path},
     * и вызывает его метод {@link IAudioMetadataOfFile#getTitle(String)}.
     * Если обработчик не найден, возвращается значение по умолчанию {@link #unkTitle}.
     * </p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * String title = getTitle("/music/song.mp3");
     * System.out.println("Название: " + title);
     * }</pre>
     *
     * @param path путь к файлу
     * @return название трека или {@code unkTitle}
     * @version 0.1.4.3
     */
    @Override
    public String getTitle(String path) {
        if(path == null)
            return null;

        IAudioMetadataOfFile format = metadataFormatsMap.get(this.getExtension(path));

        if(format != null) {
            return format.getTitle(path);
        } else {
            return "unkTitle";
        }
    }
    /**
     * Метод возвращает исполнителя из метаданных файла.
     * <p>
     * <h2>Реализация</h2>
     * Аналогично {@code getTitle}, ищет обработчик по расширению и вызывает его метод {@link IAudioMetadataOfFile#getArtist(String)}.
     * При отсутствии обработчика возвращает {@link #unkAuthor}.
     * </p>
     *
     * @param path путь к файлу
     * @return имя исполнителя или {@code unkAuthor}
     * @version 0.1.4.3
     */
    @Override
    public String getArtist(String path) {
        if(path == null)
            return null;

        IAudioMetadataOfFile format = metadataFormatsMap.get(this.getExtension(path));

        if(format != null) {
            return format.getArtist(path);
        } else {
            return "unkAuthor";
        }
    }
    /**
     * Возвращает длительность аудиофайла в секундах.
     * <p>
     * Метод ищет обработчик метаданных по расширению файла, используя карту {@link #metadataFormatsMap},
     * затем вызывает у найденного обработчика метод {@link IAudioMetadataOfFile#getAudioFileDuration(String)}.
     * Если формат не поддерживается, возвращает 0.
     * </p>
     *
     * <h2>Реализация</h2>
     * <p>Метод делегирует логику вычисления длительности конкретным форматам (MP3, WAV и др.).
     * Это позволяет централизовать работу с метаданными и расширять поддержку форматов без изменения кода.</p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * MetadataOfFile metadata = new MetadataOfFile();
     * int duration = metadata.getAudioFileDuration("/music/song.mp3");
     * System.out.println("Audio duration: " + duration + " seconds");
     * }</pre>
     *
     * @param path путь к аудиофайлу
     * @return длительность файла в секундах или 0, если формат не поддерживается
     * @version 0.1.4.4
     */
    @Override
    public int getAudioFileDuration(String path) {
        IAudioMetadataOfFile format = metadataFormatsMap.get(this.getExtension(path));

        if(format != null) {
            return format.getAudioFileDuration(path);
        } else {
            return 0;
        }
    }
    /**
     * Получает обложку (арт) аудиофайла с заданными параметрами размера и качества.
     * <p>
     * Сначала метод ищет обработчик формата по расширению файла и пытается получить изображение из метаданных.
     * Если изображение отсутствует, загружает обложку из внешнего метода {@code parseImage}.
     * При успехе, если источник не сетевой и соответствующий флаг установлен,
     * обложка сохраняется в тегах файла через {@code setAlbumArt}.
     * </p>
     *
     * <h2>Реализация</h2>
     * <p>Метод объединяет работу с метаданными и извлечение обложки с кэшированием,
     * что оптимизирует обработку и снижает количество redundant IO операций.</p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * MetadataOfFile metadata = new MetadataOfFile();
     * Image albumArt = metadata.getArt("/music/song.mp3", 200, 200, true, true);
     * imageView.setImage(albumArt);
     * }</pre>
     *
     * @param path путь к аудиофайлу
     * @param size ширина желаемого изображения
     * @param size1 высота желаемого изображения
     * @param preserve_ration сохранять пропорции изображения
     * @param smooth сглаживание при масштабировании
     * @return объект {@link Image} с обложкой или {@code null}
     * @version 0.1.4.4
     */
    @Override
    public Image getArt(Track path, int size, int size1, boolean preserve_ration, boolean smooth) {
        Image img = null;

        IAudioMetadataOfFile format = metadataFormatsMap.get(this.getExtension(path.getPath()));

        if (format != null) {
            img = format.getArt(path, size, size1, preserve_ration, smooth);
        }

        if (img == null) {
            img = Root.rootImpl.artProcessor.parseImage(path.getName(), size, size1, preserve_ration, smooth);

            if (!PlayProcessor.playProcessor.isNetwork() && ConfigurationManager.instance.getBooleanItem("album_art_parsed_set_in_tags", "false")) {
                setArt(path.getPath(), SwingFXUtils.fromFXImage(img, null));
            }
        }

        return img;
    }
    /**
     * Устанавливает название трека в метаданные аудиофайла.
     * <p>
     * Метод получает расширение файла из {@code path}, ищет обработчик формата
     * в {@link #metadataFormatsMap} и делегирует вызов {@link IAudioMetadataOfFile#setTitle(String, String)}.
     * Если формат не поддерживается, операция игнорируется.
     * </p>
     *
     * <h2>Реализация</h2>
     * <p>Делегирование установки названия формату позволяет централизованно управлять
     * различными форматами файлов без дублирования логики.</p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * MetadataOfFile metadata = new MetadataOfFile();
     * metadata.setTitle("/music/song.mp3", "New Title");
     * }</pre>
     *
     * @param path путь к аудиофайлу
     * @param title новое название трека
     * @version 0.1.4.4
     */
    @Override
    public void setTitle(String path, String title) {
        IAudioMetadataOfFile format = metadataFormatsMap.get(this.getExtension(path));

        if(format != null) {
            format.setTitle(path, title);
        }
    }
    /**
     * Устанавливает имя исполнителя в метаданные аудиофайла.
     * <p>
     * Аналогично {@link #setTitle(String, String)} метод делегирует вызов по формату файла.
     * </p>
     *
     * <h2>Реализация</h2>
     * <p>Позволяет централизованно обновлять исполнителя для поддерживаемых форматов.</p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * MetadataOfFile metadata = new MetadataOfFile();
     * metadata.setArtist("/music/song.mp3", "New Artist");
     * }</pre>
     *
     * @param path путь к аудиофайлу
     * @param artist новое имя исполнителя
     * @version 0.1.4.4
     */
    @Override
    public void setArtist(String path, String artist) {
        IAudioMetadataOfFile format = metadataFormatsMap.get(this.getExtension(path));

        if(format != null) {
            format.setArtist(path, artist);
        }
    }

    /**
     * Устанавливает обложку (арт) аудиофайла.
     * <p>
     * Делегирует вызов обработки конкретному формату из {@link #metadataFormatsMap}.
     * </p>
     *
     * <h2>Реализация</h2>
     * <p>Обновление изображения обложки происходит в формате, поддерживающем запись метаданных.</p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * BufferedImage img = ImageIO.read(new File("/path/to/image.png"));
     * MetadataOfFile metadata = new MetadataOfFile();
     * metadata.setArt("/music/song.mp3", img);
     * }</pre>
     *
     * @param path путь к аудиофайлу
     * @param image изображение обложки в формате BufferedImage
     * @version 0.1.4.4
     */
    @Override
    public void setArt(String path, BufferedImage image) {
        IAudioMetadataOfFile format = metadataFormatsMap.get(this.getExtension(path));

        if(format != null) {
            format.setArt(path, image);
        }
    }
    /**
     * Получает значение метаданных по ключу.
     * <p>
     * Делегирует вызов конкретному формату. Если формат не поддерживается — возвращает null.
     * </p>
     *
     * <h2>Реализация</h2>
     * <p>Унификация чтения произвольных тегов по ключу для разных форматов.</p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * String genre = metadata.getMetadataValue("/music/song.mp3", "genre");
     * }</pre>
     *
     * @param path путь к файлу
     * @param key ключ метаданных
     * @return значение по ключу или null
     * @version 0.1.4.4
     */
    @Override
    public String getMetadataValue(String path, String key) {
        IAudioMetadataOfFile format = metadataFormatsMap.get(this.getExtension(path));

        if(format != null) {
           return format.getMetadataValue(path, key);
        } else {
            return null;
        }
    }
    /**
     * Устанавливает значение метаданных по ключу.
     * <p>
     * Делегирует действие соответствующему формату из {@link #metadataFormatsMap}.
     * </p>
     *
     * <h2>Реализация</h2>
     * <p>Позволяет динамично изменять произвольные метаданные для поддержки разных форматов.</p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * metadata.setMetadataValue("/music/song.mp3", "genre", "Rock");
     * }</pre>
     *
     * @param path путь к аудиофайлу
     * @param key ключ метаданных
     * @param value новое значение
     * @version 0.1.4.4
     */
    @Override
    public void setMetadataValue(String path, String key, String value) {
        IAudioMetadataOfFile format = metadataFormatsMap.get(this.getExtension(path));

        if(format != null) {
            format.setMetadataValue(path, key, value);
        }
    }
    /**
     * Возвращает список всех пар ключ-значение метаданных из файла.
     * <p>
     * Делегирует вызов методу {@link IAudioMetadataOfFile#getAllMetadata(String)} конкретного формата.
     * Если формат не поддерживается, возвращает пустой список.
     * </p>
     *
     * <h2>Реализация</h2>
     * <p>Обеспечивает универсальный доступ ко всем метаданным для разных форматов.</p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * List<Map.Entry<String, String>> metadataList = metadata.getAllMetadata("/music/song.mp3");
     * for (Map.Entry<String,String> entry : metadataList) {
     *     System.out.println(entry.getKey() + ": " + entry.getValue());
     * }
     * }</pre>
     *
     * @param path путь к аудиофайлу
     * @return список всех метаданных, возможно пустой
     * @version 0.1.4.4
     */
    @Override
    public List<Map.Entry<String, String>> getAllMetadata(String path) {
        IAudioMetadataOfFile format = metadataFormatsMap.get(this.getExtension(path));

        if(format != null) {
            return format.getAllMetadata(path);
        } else {
            return List.of();
        }
    }
    /**
     * Вспомогательный метод для извлечения расширения файла из пути.
     * <p>
     * Возвращает строку после последней точки в имени файла; если точки нет, возвращается код URI_NULL.
     * </p>
     *
     * <h2>Реализация</h2>
     * <p>Используется для определения формата файла и выбора соответствующего обработчика метаданных.</p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * String ext = getExtension("/music/song.mp3"); // "mp3"
     * String ext2 = getExtension("/file_without_ext"); // URI_NULL.getCode()
     * }</pre>
     *
     * @param path путь к файлу
     * @return расширение файла или специальный код
     * @version 0.1.4.4
     */
    private String getExtension(String path) {
        if(!path.contains("."))
            return URI_NULL.getCode();

        return path.substring(path.lastIndexOf(".") + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        MetadataOfFile that = (MetadataOfFile) o;
        return metadataFormatsMap.equals(that.metadataFormatsMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadataFormatsMap);
    }

    @Override
    public String toString() {
        return "MetadataOfFile{" +
                "metadataFormatsMap=" + metadataFormatsMap +
                '}';
    }
}