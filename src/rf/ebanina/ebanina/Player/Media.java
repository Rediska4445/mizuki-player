package rf.ebanina.ebanina.Player;

import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.utils.loggining.logging;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

/**
 * <h1>Media</h1>
 * Класс, представляющий аудиомедиа-ресурс, используемый в медиаплеере.
 * <p>
 * {@code Media} является легковесной обёрткой, инкапсулирующей источник аудиоданных (файл или сетевой URI)
 * и опциональную аудиодорожку интро. Он не содержит логики воспроизведения — его задача — предоставить
 * необходимые данные для класса {@link MediaPlayer}, который использует этот объект для инициализации
 * и управления воспроизведением.
 * </p>
 * <p>
 * Поддерживаемые типы источников:
 * <ul>
 *   <li><b>Локальные файлы</b> — указываются через путь к файлу или URI с префиксом {@code file:}.</li>
 *   <li><b>Сетевые ресурсы</b> — указываются через URI (например, {@code http://}, {@code https://}).</li>
 * </ul>
 * </p>
 * <p>
 * Особое внимание уделяется поддержке аудиодорожки интро — дополнительного звука, проигрываемого
 * перед основным медиа-ресурсом. Если файл интро задан через {@link #setIntroSoundFile(Path)},
 * {@link MediaPlayer} будет использовать его при воспроизведении. Удаление или отсутствие файла интро
 * приведёт к тому, что интро воспроизводиться не будет. Источник изменять динамически нет смысла -
 * {@link MediaPlayer} загружает его в память сразу. Класс не должен наследоваться - {@code final}, в ином решении нет смысла,
 * эта обёртка итак излишняя и создана ради расширяемости и удобства
 * </p>
 * <p>
 * Класс является неизменяемым по источнику ({@code source}), но допускает изменение файла интро
 * в течение жизненного цикла. Это позволяет динамически управлять наличием интро без пересоздания
 * самого объекта {@code Media}.
 * </p>
 * <p>
 * Экземпляры {@code Media} сравниваются по полю {@code source} — два объекта считаются равными,
 * если их источники идентичны. Это позволяет использовать {@code Media} в коллекциях, таких как
 * {@code HashSet} или {@code HashMap}, для отслеживания уже загруженных ресурсов.
 * </p>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * // Создание из строки-пути
 * Media media = new Media("/music/song.mp3");
 *
 * // Создание из файла
 * Media media = new Media(new File("/music/song.mp3"));
 *
 * // Создание из URI
 * Media media = new Media(URI.create("http://example.com/stream.mp3"));
 *
 * // Установка интро
 * media.setIntroSoundFile(new File("/sounds/intro.wav"));
 * }</pre>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li>Класс не проверяет существование или доступность ресурса при создании.</li>
 *   <li>Изменение файла интро не влияет на уже начатое воспроизведение — эффект проявится при следующем запуске.</li>
 * </ul>
 *
 * @author Ebanina Std.
 * @version 0.1.4.3
 * @since 0.1.3.0
 * @see MediaPlayer
 * @see File
 * @see URI
 */
@logging(tag = "Media", isActive = false, fileOut = false)
public final class Media
        implements Serializable, MediaReferencable, Comparable<Media>
{
    /**
     * Основной и неизменяемый источник медиа-ресурса, представляющий собой строку-идентификатор.
     * <p>
     * Это ключевое поле класса, определяющее, какой аудиоресурс будет использован {@link MediaPlayer}.
     * Оно не зависит от конкретной реализации хранения — может представлять как локальный файл,
     * так и сетевой URI, и интерпретируется {@link MediaPlayer} на этапе воспроизведения.
     * </p>
     * <p>
     * Особенности:
     * <ul>
     *   <li><b>Неизменяемо</b> — устанавливается только в конструкторе и помечено как {@code final}.</li>
     *   <li><b>Универсально</b> — поддерживает любые валидные строки, интерпретируемые как ресурс
     *       (например, {@code file:/path/to/file.mp3}, {@code http://example.com/stream.mp3}).</li>
     *   <li><b>Центрально</b> — используется во всех перегрузках конструкторов; именно его валидность
     *       критична для корректной работы объекта.</li>
     *   <li><b>Определяет тип ресурса</b> — {@link MediaPlayer} сам анализирует строку, чтобы определить,
     *       является ли ресурс локальным или сетевым (через префикс, например).</li>
     * </ul>
     * </p>
     * <p>
     * Поскольку {@code source} используется как идентификатор в {@link #equals(Object)} и {@link #hashCode()},
     * он определяет уникальность медиа-ресурса в коллекциях и кэшах.
     * </p>
     *
     * @see #Media(String)
     * @see #Media(File)
     * @see #Media(URI)
     * @see #getSource()
     * @see #getURI()
     * @see #getFile()
     * @see MediaPlayer
     * @since 0.1.3.0
     */
    private final String source;

    /**
     * Опциональный файл аудиодорожки интро, проигрываемой перед основным медиа-ресурсом.
     * <p>
     * Начиная с версии {@code 0.1.3.2}, это поле позволяет задать звуковое сопровождение
     * для начала воспроизведения. Используется исключительно как путь к <b>локальному файлу</b> —
     * нет необходимости создавать отдельный объект {@code Media} или сложную логику в {@link MediaPlayer},
     * так как интро — простой аудиофайл, загружаемый напрямую.
     * </p>
     * <p>
     * Особенности:
     * <ul>
     *   <li><b>Не является сетевым ресурсом</b> — поддерживает только локальные файлы через {@link Path}.</li>
     *   <li><b>Не final</b> — может быть изменён в течение жизненного цикла объекта {@code Media},
     *       что позволяет динамически управлять наличием интро.</li>
     *   <li><b>Опционален</b> — если значение {@code null}, интро не воспроизводится.</li>
     *   <li><b>Загружается единожды</b> — {@link MediaPlayer} читает файл при старте воспроизведения
     *       и не отслеживает последующие изменения.</li>
     * </ul>
     * </p>
     * <p>
     * Поле может стать {@code final} в будущих версиях, если будет принято решение
     * о фиксации интро на этапе создания объекта. Пока сохраняется гибкость.
     * </p>
     *
     * @implNote
     * Для внутреннего использования предпочтительно {@link #getIntroSound()}, возвращающий {@link Path}.
     * Метод {@link #getIntroSoundFile()} сохранён для обратной совместимости с кодом,
     * ожидающим {@link File}.
     *
     * @see #setIntroSoundFile(Path)
     * @see #getIntroSound()
     * @see #getIntroSoundFile()
     * @see MediaPlayer
     * @since 0.1.3.2
     */
    private Intro introSoundFile;

    /**
     * Флаг, определяющий тип источника медиа-ресурса: локальный или сетевой.
     * <p>
     * Это поле служит оптимизационной подсказкой для {@link MediaPlayer}, позволяя ему
     * заранее выбрать подходящую стратегию загрузки и воспроизведения без необходимости
     * анализировать строку {@link #source} во время выполнения.
     * </p>
     * <p>
     * Логика определения:
     * <ul>
     *   <li><b>{@code true}</b> — источник является сетевым ресурсом (например, HTTP/HTTPS-поток).</li>
     *   <li><b>{@code false}</b> — источник является локальным файлом или {@code file:} URI.</li>
     * </ul>
     * </p>
     * <p>
     * Особенности:
     * <ul>
     *   <li><b>Привязка</b> — устанавливается только в конструкторе, поскольку привязано
     *       к неизменяемому полю {@link #source}.</li>
     *   <li><b>Устанавливается явно</b> — в специальных конструкторах типа {@code Media(URL, boolean)}
     *       можно передать значение напрямую, минуя автоматическое определение по URL-схеме.</li>
     *   <li><b>Не участвует в сравнении</b> — {@link #equals(Object)} основан только на {@link #source},
     *       так как {@code isNetty} является производной характеристикой.</li>
     * </ul>
     * </p>
     * <p>
     * Поле используется {@link MediaPlayer} для:
     * <ul>
     *   <li>Выбора подходящего загрузчика (локальный {@code FileInputStream} vs сетевой {@code URLConnection}).</li>
     *   <li>Настройки буферизации и кэширования.</li>
     *   <li>Обработки ошибок сети и таймаутов.</li>
     * </ul>
     * </p>
     *
     * @implNote
     * В конструкторах {@link #Media(String)}, {@link #Media(File)} и {@link #Media(URI)}
     * значение устанавливается автоматически на основе схемы URI. В будущих версиях могут быть
     * добавлены конструкторы с явным указанием флага для особых случаев.
     *
     * @see #Media(String)
     * @see #Media(File)
     * @see #Media(URI)
     * @see #isNetty()
     * @see #getSource()
     * @see MediaPlayer
     * @since 0.1.4.3
     */
    private final boolean isNetty;

    /**
     * Возвращает флаг, указывающий, является ли источник медиа-ресурса сетевым.
     * <p>
     * Это оптимизационная метка, используемая {@link MediaPlayer} для выбора стратегии
     * загрузки и воспроизведения без необходимости анализа строки {@link #source} при каждом вызове.
     * </p>
     * <p>
     * Логика:
     * <ul>
     *   <li>{@code true} — ресурс является сетевым (например, HTTP/HTTPS-поток).</li>
     *   <li>{@code false} — ресурс является локальным файлом или {@code file:} URI.</li>
     * </ul>
     * </p>
     * <p>
     * Значение устанавливается в конструкторе и не изменяется в течение жизненного цикла объекта.
     * </p>
     *
     * @return {@code true}, если источник — сетевой ресурс; иначе {@code false}
     * @see #isNetty
     * @see #Media(String)
     * @see #Media(File)
     * @see #Media(URI)
     * @since 0.1.4.3
     */
    public boolean isNetty() {
        return isNetty;
    }

    /**
     * Создаёт объект {@code Media} с заданным источником в виде строки.
     * <p>
     * Источник может быть:
     * <ul>
     *   <li>Путём к локальному файлу (например, {@code /music/song.mp3}).</li>
     *   <li>URI (например, {@code file:/path/to/file.mp3}, {@code http://example.com/stream.mp3}).</li>
     * </ul>
     * </p>
     * <p>
     * Для сетевых ресурсов рекомендуется использовать конструктор с {@link URI}.
     * </p>
     * <p>
     * Автоматически устанавливается файл интро по умолчанию, загружаемый через
     * {@link ResourceManager}.
     * </p>
     *
     * @param source строка с URI или путём к аудиофайлу; не должна быть {@code null}
     * @throws NullPointerException если {@code source} равен {@code null}
     * @see #setIntroSoundFile(Path)
     * @see ResourceManager
     * @since 0.1.3.0
     */
    public Media(String source, boolean isNetty) {
        this(source, ResourceManager.Instance.resourcesPaths.get(Resources.ID.AUDIO_INTRO_FILE), isNetty);
    }

    /**
     * Создаёт объект {@code Media} с заданным источником в виде строки.
     * <p>
     * Источник может быть:
     * <ul>
     *   <li>Путём к локальному файлу (например, {@code /music/song.mp3}).</li>
     *   <li>URI (например, {@code file:/path/to/file.mp3}, {@code http://example.com/stream.mp3}).</li>
     * </ul>
     * </p>
     * <p>
     * Для сетевых ресурсов рекомендуется использовать конструктор с {@link URI}.
     * </p>
     * <p>
     * Автоматически устанавливается файл интро по умолчанию, загружаемый через
     * {@link ResourceManager}.
     * </p>
     *
     * @param source строка с URI или путём к аудиофайлу; не должна быть {@code null}
     * @param audioIntro путь к файлу интро
     * @throws NullPointerException если {@code source} равен {@code null}
     * @see #setIntroSoundFile(Path)
     * @see ResourceManager
     * @since 0.1.3.0
     */
    public Media(String source, String audioIntro, boolean isNetty) {
        this.source = source;
        this.isNetty = isNetty;

        if(audioIntro != null) {
            this.introSoundFile = new Intro(new File(audioIntro).toPath());
        }
    }

    /**
     * Создаёт объект {@code Media} с заданным источником в виде строки.
     * <p>
     * Источник может быть:
     * <ul>
     *   <li>Путём к локальному файлу (например, {@code /music/song.mp3}).</li>
     *   <li>URI (например, {@code file:/path/to/file.mp3}, {@code http://example.com/stream.mp3}).</li>
     * </ul>
     * </p>
     * <p>
     * Флаг {@link #isNetty} устанавливается в {@code false} по умолчанию, так как строка
     * не анализируется на наличие сетевых схем — предполагается локальный ресурс.
     * Для сетевых ресурсов рекомендуется использовать конструктор с {@link URI}.
     * </p>
     * <p>
     * Автоматически устанавливается файл интро по умолчанию, загружаемый через
     * {@link ResourceManager}.
     * </p>
     *
     * @param source строка с URI или путём к аудиофайлу; не должна быть {@code null}
     * @throws NullPointerException если {@code source} равен {@code null}
     * @see #setIntroSoundFile(Path)
     * @see ResourceManager
     * @since 0.1.3.0
     */
    public Media(String source) {
        this(source, false);
    }

    /**
     * Создаёт объект {@code Media} из локального файла.
     * <p>
     * Источник устанавливается как {@code file:} URI, созданный из переданного файла.
     * Флаг {@link #isNetty} автоматически устанавливается в {@code false}, так как
     * ресурс является локальным.
     * </p>
     * <p>
     * Автоматически устанавливается файл интро по умолчанию.
     * </p>
     *
     * @param file файл с аудио; не должен быть {@code null}
     * @throws NullPointerException если {@code file} равен {@code null}
     * @see #Media(String, boolean)
     * @see #getSource()
     * @see #isNetty()
     * @since 0.1.4.3
     */
    public Media(File file) {
        this(file.toURI().toString(), false);
    }

    /**
     * Создаёт объект {@code Media} из локального файла.
     * <p>
     * Источник устанавливается как {@code file:} URI, созданный из переданного файла.
     * Флаг {@link #isNetty} автоматически устанавливается в {@code false}, так как
     * ресурс является локальным.
     * </p>
     * <p>
     * Автоматически устанавливается файл интро по умолчанию.
     * </p>
     *
     * @param file файл с аудио; не должен быть {@code null}
     * @throws NullPointerException если {@code file} равен {@code null}
     * @see #Media(String, boolean)
     * @see #getSource()
     * @see #isNetty()
     * @since 0.1.4.3
     */
    public Media(Path file) {
        this(file.toUri().toString(), false);
    }

    /**
     * Создаёт объект {@code Media} из URI.
     * <p>
     * Поддерживает как локальные ({@code file:}), так и сетевые ({@code http://}, {@code https://})
     * ресурсы. Флаг {@link #isNetty} устанавливается автоматически:
     * <ul>
     *   <li>{@code true} — если схема URI не является {@code file:}.</li>
     *   <li>{@code false} — если схема — {@code file:}.</li>
     * </ul>
     * </p>
     * <p>
     * Это позволяет {@link MediaPlayer} заранее определить тип ресурса и выбрать
     * подходящую стратегию загрузки.
     * </p>
     * <p>
     * Автоматически устанавливается файл интро по умолчанию.
     * </p>
     *
     * @param uri URI с аудиоресурсом; не должен быть {@code null}
     * @throws NullPointerException если {@code uri} равен {@code null}
     * @see #Media(String, boolean)
     * @see #getSource()
     * @see #isNetty()
     * @since 0.1.4.3
     */
    public Media(URI uri) {
        this(uri.toString(), !uri.getScheme().equalsIgnoreCase("file"));
    }

    /**
     * Создаёт объект {@code Media} из URI.
     * <p>
     * Поддерживает только сетевые ({@code http://}, {@code https://})
     * ресурсы. Флаг {@link #isNetty} устанавливается автоматически на {@code true}:
     * </p>
     * <p>
     * Это позволяет {@link MediaPlayer} заранее определить тип ресурса и выбрать
     * подходящую стратегию загрузки.
     * </p>
     * <p>
     * Автоматически устанавливается файл интро по умолчанию.
     * </p>
     *
     * @param uri URL с аудиоресурсом; не должен быть {@code null}
     * @throws NullPointerException если {@code uri} равен {@code null}
     * @see #Media(String, boolean)
     * @see #getSource()
     * @see #isNetty()
     * @since 0.1.4.3
     */
    public Media(URL uri) {
        this(uri.toString(), true);
    }

    public Media setIntroSoundFile(Intro introSoundFile) {
        this.introSoundFile = introSoundFile;
        return this;
    }

    public Intro getIntro() {
        return introSoundFile;
    }

    /**
     * Устанавливает файл аудиодорожки интро, проигрываемой перед основным медиа-ресурсом.
     * <p>
     * Интро используется {@link MediaPlayer} только если файл существует и доступен.
     * Установка {@code null} отключает воспроизведение интро.
     * </p>
     * <p>
     * Изменение значения не влияет на уже начатое воспроизведение — новый файл будет
     * загружен при следующем вызове {@link MediaPlayer#play()}.
     * </p>
     *
     * @param file путь к аудиофайлу интро или {@code null} для отключения
     * @see #getIntroSound()
     * @see #getIntroSoundFile()
     * @since 0.1.3.2
     */
    public void setIntroSoundFile(Path file) {
        this.introSoundFile = new Intro(file);
    }

    /**
     * Возвращает файл аудиодорожки интро в виде {@link File}.
     * <p>
     * Метод предоставлен для обратной совместимости с кодом, ожидающим {@link File}.
     * Для нового кода предпочтительнее использовать {@link #getIntroSound()}.
     * </p>
     *
     * @return файл интро или {@code null}, если интро не задано
     * @see #getIntroSound()
     * @see #setIntroSoundFile(Path)
     * @since 0.1.3.2
     */
    public File getIntroSoundFile() {
        return introSoundFile.getFileSource();
    }

    /**
     * Возвращает путь к файлу аудиодорожки интро в виде {@link Path}.
     * <p>
     * Предпочтительный способ получения пути к интро, так как {@link Path} является
     * современным и более гибким API для работы с файловой системой.
     * </p>
     *
     * @return путь к файлу интро или {@code null}, если интро не задано
     * @see #getIntroSoundFile()
     * @see #setIntroSoundFile(Path)
     * @since 0.1.3.2
     */
    public Path getIntroSound() {
        return introSoundFile.getFileSource().toPath();
    }

    /**
     * Возвращает строку-источник медиа-ресурса, использованную при создании объекта.
     * <p>
     * Это основной идентификатор медиа-ресурса, определяющий, какой файл или поток
     * будет передан {@link MediaPlayer} для воспроизведения.
     * </p>
     * <p>
     * Значение неизменно в течение жизненного цикла объекта.
     * </p>
     *
     * @return исходная строка-источник (URI или путь к файлу); никогда не возвращает {@code null}
     * @see #getURI()
     * @see #getFile()
     * @see #getName()
     * @since 0.1.3.0
     */
    public String getSource() {
        return source;
    }

    /**
     * Возвращает URI, сформированный из строки-источника.
     * <p>
     * Используется {@link MediaPlayer} для унифицированного доступа к ресурсу,
     * независимо от его формата (локальный путь или URL).
     * </p>
     *
     * @return URI аудиоданных; никогда не возвращает {@code null}
     * @throws IllegalArgumentException если строка источника не является валидным URI
     * @see #getSource()
     * @since 0.1.4.3
     */
    public URI getURI() {
        return URI.create(source);
    }

    /**
     * Возвращает объект {@link File}, если источник представляет собой локальный файл.
     * <p>
     * Метод пытается интерпретировать источник как локальный путь:
     * <ul>
     *   <li>Если источник начинается с {@code file:}, преобразует его в {@link File}.</li>
     *   <li>Если источник — обычный путь, проверяет существование файла.</li>
     * </ul>
     * </p>
     *
     * @return объект {@link File}, если источник — существующий локальный файл; иначе {@code null}
     * @see #getSource()
     * @see #getURI()
     * @since 0.1.3.0
     */
    public File getFile() {
        if (source.startsWith("file:")) {
            return new File(getURI());
        }
        File f = new File(source);
        return f.exists() ? f : null;
    }

    /**
     * Возвращает имя файла аудиоданных без пути.
     * <p>
     * Если источник представляет собой локальный файл, возвращается его имя.
     * В противном случае (например, сетевой поток) возвращается сама строка-источник.
     * </p>
     * <p>
     * Используется для отображения названия трека в интерфейсе, когда метаданные недоступны.
     * </p>
     *
     * @return имя файла или строка-источник; никогда не возвращает {@code null}
     * @see #getName()
     * @see #getSource()
     * @since 0.1.3.0
     */
    public String getName() {
        File f = getFile();
        if (f != null)
            return f.getName();

        return source;
    }

    /**
     * Сравнивает этот объект {@code Media} с указанным на равенство.
     * <p>
     * Два объекта {@code Media} считаются равными, если их источники ({@link #source}) идентичны.
     * Сравнение основано исключительно на строке-источнике, так как она является уникальным
     * идентификатором медиа-ресурса.
     * </p>
     * <p>
     * Файл интро ({@link #introSoundFile}) и флаг {@link #isNetty} не участвуют в сравнении,
     * так как не влияют на идентичность ресурса.
     * </p>
     * <p>
     * Этот метод согласован с {@link #hashCode()} и обеспечивает корректную работу
     * в коллекциях, таких как {@code HashSet} и {@code HashMap}.
     * </p>
     *
     * @param o объект для сравнения
     * @return {@code true}, если указанный объект равен этому {@code Media}; иначе {@code false}
     * @see #hashCode()
     * @see #getSource()
     * @since 0.1.4.0
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Media media = (Media) o;
        return source.equals(media.getSource());
    }

    /**
     * Возвращает хеш-код для этого объекта {@code Media}.
     * <p>
     * Хеш-код вычисляется на основе строки-источника ({@link #source}), так как именно она
     * определяет уникальность медиа-ресурса.
     * </p>
     * <p>
     * Метод согласован с {@link #equals(Object)}: если два объекта равны, их хеш-коды также равны.
     * Это необходимо для корректной работы в хэш-коллекциях.
     * </p>
     *
     * @return хеш-код, основанный на {@link #source}
     * @see #equals(Object)
     * @since 0.1.4.0
     */
    @Override
    public int hashCode() {
        return Objects.hash(source);
    }

    /**
     * Возвращает строковое представление объекта {@code Media}.
     * <p>
     * Строка содержит ключевые поля в формате:
     * <pre>{@code Media{source='...', introSoundFile=...}}</pre>
     * </p>
     * <p>
     * Предназначено для отладки и логирования. Не предназначен для парсинга.
     * </p>
     * <p>
     * Поле {@link #isNetty} не включено, так как является производным от {@link #source}
     * и не добавляет полезной информации для отладки.
     * </p>
     *
     * @return строковое представление объекта
     * @since 0.1.3.0
     */
    @Override
    public String toString() {
        return "Media{" +
                "source='" + source + '\'' +
                ", introSoundFile=" + introSoundFile +
                '}';
    }

    /**
     * Сравнивает этот объект {@code Media} с указанным в естественном порядке.
     * <p>
     * Сортировка выполняется по строке-источнику ({@link #source}) в лексикографическом порядке.
     * Это обеспечивает стабильный и предсказуемый порядок сортировки медиа-ресурсов.
     * </p>
     * <p>
     * Метод согласован с {@link #equals(Object)}: если {@code a.equals(b)}, то
     * {@code a.compareTo(b) == 0}.
     * </p>
     * <p>
     * Используется в сортируемых коллекциях, таких как {@code TreeSet} и {@code TreeMap}.
     * </p>
     *
     * @param o объект для сравнения
     * @return отрицательное число, ноль или положительное число, если этот объект
     *         меньше, равен или больше указанного
     * @throws ClassCastException если указанный объект не является экземпляром {@code Media}
     * @throws NullPointerException если указанный объект — {@code null}
     * @see #equals(Object)
     * @see Comparable
     * @since 0.1.4.3
     */
    @Override
    public int compareTo(Media o) {
        if (this == o)
            return 0;

        if (o == null) {
            throw new ClassCastException("Cannot compare Media with null");
        }

        return this.source.compareTo((o).getSource());
    }

    public String getPath() {
        return source;
    }

    @Override
    public String path() {
        return source;
    }
}