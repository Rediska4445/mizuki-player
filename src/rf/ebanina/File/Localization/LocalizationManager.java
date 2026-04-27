package rf.ebanina.File.Localization;

import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Resources.ResourceManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <h1>LocalizationManager</h1>
 *
 * <p>Синглтон-класс для управления локализацией интерфейса.
 * Предоставляет доступ к доступным языкам и переведённым строкам,
 * читая их из ресурсов.</p>
 *
 * <p>Работает с {@link ConfigurationManager}, {@link ResourceManager} и {@link FileManager} для
 * получения пути к ресурсам и загрузки переводов.</p>
 *
 * <h2>Особенности реализации</h2>
 * <ul>
 *   <li>Текущий язык определяется по конфигурации или автоматически в случае "auto".</li>
 *   <li>Файлы локализации расположены в папке, указанной в {@link ResourceManager}.</li>
 *   <li>Переводы кешируются в {@link #locale_map} для оптимизации.</li>
 *   <li>Исключения при работе с файловой системой не подавляются — должны обрабатываться вызывающим кодом.</li>
 * </ul>
 *
 * <h2>
 *     <strong> Является внутренним модулём {@link ResourceManager} </strong>
 * </h2>
 *
 * <h2>Пример использования</h2>
 * <pre>{@code
 * String translated = LocalizationManager.instance.getLocalizationString("hello", "default value");
 * }</pre>
 *
 * @author Ebanina Std.
 * @version 1.4.12
 * @since 1.2.5
 */
public class LocalizationManager
{
    /**
     * Инкапсулированная ссылка на ресурсный менеджер. Нужен для DI.
     * <p>
     * Это поле обеспечивает единственный доступный во всём классе к работе с ресурсами,
     * который создаётся при загрузке класса.
     * </p>
     * <p>
     * Особенности:
     * <ul>
     *   <li><b>Это ссылка</b> — это нужно для возможности создания собственного {@link FileManager}.</li>
     *   <li><b>Приватный доступ</b> — поле предоставляет единичный экземпляр для класса.</li>
     * </ul>
     * </p>
     *
     * @see ResourceManager
     */
    protected final String resourceLanguageFile;
    /**
     * Инкапсулированная ссылка на файловый менеджер. Нужен для DI.
     * <p>
     * Это поле обеспечивает единственный доступный во всём классе к работе с файлами,
     * который создаётся при загрузке класса.
     * </p>
     * <p>
     * Особенности:
     * <ul>
     *   <li><b>Это ссылка</b> — это нужно для возможности создания собственного {@link FileManager}.</li>
     *   <li><b>Приватный доступ</b> — поле предоставляет единичный экземпляр для класса.</li>
     * </ul>
     * </p>
     *
     * @see FileManager
     */
    private final FileManager fileManager;
    /**
     * Конструктор по умолчанию для {@code LocalizationManager}.
     * <p>
     * Создаёт новый экземпляр менеджера локализации.
     * Конструктор публичный, но класс предполагается использовать через синглтон.
     * </p>
     */
    public LocalizationManager(FileManager fileManager, String resourceLanguageFile, String lang, Path langPath) {
        this.fileManager = fileManager;
        this.langPath = langPath;
        this.lang = getCurrentLocaleLanguage(lang);
        this.resourceLanguageFile = resourceLanguageFile;
    }
    /**
     * Текущий язык локализации.
     * <p>
     * Значение инициализируется вызовом {@link #getCurrentLocaleLanguage(String)},
     * определяющего язык на основании конфигураций.
     * </p>
     */
    protected String lang;

    public String getLang() {
        return lang;
    }

    /**
     * Текущий путь к файлу с языком.
     * <p>
     * Значение инициализируется вызовом {@link #getCurrentLocaleLanguage(String)},
     * определяющего файл на основании конфигураций.
     * </p>
     */
    public Path langPath;

    private String locale;

    public String getLocale() {
        return locale;
    }

    /**
     * Определяет текущий язык локализации на основе конфигурационных настроек.
     * <p>
     * Метод извлекает значение ключа {@code "lang"} из {@link ConfigurationManager}.
     * Если значение равно {@code "auto"} (без учёта регистра), язык определяется автоматически
     * вызовом {@link #identifyLocale()}. В противном случае возвращается указанное в конфигурации значение.
     * </p>
     *
     * <h3>Версия</h3>
     * <ul>
     *   <li>Введён в версии: 1.4</li>
     *   <li>Текущая версия реализации: 1.4.4</li>
     * </ul>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * String currentLang = getCurrentLocaleLanguage();
     * System.out.println("Current locale language: " + currentLang);
     * }</pre>
     *
     * <h3>Особенности</h3>
     * <ul>
     *   <li>Возвращает строку языка локализации в формате типа "en_en".</li>
     *   <li>Поддерживает автоматическое определение языка при значении "auto".</li>
     *   <li>Использует {@code ConfigurationManager.instance} как источник настроек.</li>
     *   <li>Метод приватный, используется для инициализации текущей локали.</li>
     * </ul>
     *
     * @return строка с кодом текущего языка локализации
     */
    protected String getCurrentLocaleLanguage(String defaultLang) {
        if(defaultLang.equalsIgnoreCase("auto")) {
            return locale = identifyLocale();
        }

        return locale = defaultLang;
    }
    /**
     * Карта доступных языков локализации.
     * <p>
     * Ключ — двухбуквенный код языка (например, {@code "en"}), значение — полное имя файла локализации
     * (например, {@code "en_us"}).
     * </p>
     *
     * <h3>Версия</h3>
     * <ul>
     *   <li>Введена в версии: 1.4</li>
     *   <li>Текущая версия реализации: 1.4.4</li>
     * </ul>
     *
     * <h3>Особенности</h3>
     * <ul>
     *   <li>Инициализируется при загрузке из метода {@link #getAvailableLanguage()}.</li>
     *   <li>Обеспечивает быстрый доступ к списку поддерживаемых языков.</li>
     *   <li>Используется для выбора локального файла перевода.</li>
     * </ul>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * // Вывести все доступные языки и соответствующие локали
     * for (Map.Entry<String, String> entry : availableLanguage.entrySet()) {
     *     System.out.println("Код языка: " + entry.getKey() + ", Локаль: " + entry.getValue());
     * }
     * }</pre>
     */
    public Map<String, String> availableLanguage;
    /**
     * Сканирует папку с языковыми ресурсами и возвращает карту доступных языков.
     * <p>
     * Сканирование происходит в папке, путь к которой берётся из {@link ResourceManager}.
     * Имена файлов локализации должны иметь формат {@code <код_языка>_<страна>.lang}.
     * Из имени формируется ключ (код языка) и значение (полное имя локали без расширения).
     * </p>
     *
     * <h3>Версия</h3>
     * <ul>
     *   <li>Введен в версии: 1.4.1</li>
     *   <li>Текущая версия реализации: 1.4.4</li>
     * </ul>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Map<String, String> langs = getAvailableLanguage();
     * langs.forEach((code, locale) -> System.out.println(code + " -> " + locale));
     * }</pre>
     *
     * <h3>Особенности</h3>
     * <ul>
     *   <li>Если путь к папке локалей не найден — выбрасывается {@link IllegalStateException}.</li>
     *   <li>Если папка локалей отсутствует или не директория — выбрасывается {@link IllegalArgumentException}.</li>
     *   <li>При ошибках ввода-вывода бросается {@link RuntimeException} обёртка над {@link IOException}.</li>
     * </ul>
     *
     * @return карта с доступными языками локализации
     * @throws IllegalStateException если путь к локализациям отсутствует в {@link ResourceManager}
     * @throws IllegalArgumentException если путь указан неверно или не является директорией
     * @throws RuntimeException при ошибках чтения файлов
     */
    protected Map<String, String> getAvailableLanguage() {
        Map<String, String> languageMap = new LinkedHashMap<>();

        String langFolderPath = langPath.toString();

        Path langDir = Paths.get(langFolderPath);
        if (!Files.exists(langDir) || !Files.isDirectory(langDir)) {
            throw new IllegalArgumentException("Invalid lang directory: " + langFolderPath);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(langDir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    String filename = entry.getFileName().toString();

                    int dotIndex = filename.indexOf('.');
                    String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;

                    int underscoreIndex = baseName.indexOf('_');
                    if (underscoreIndex > 0) {
                        String key = baseName.substring(0, underscoreIndex).toLowerCase();
                        languageMap.put(key, baseName);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return languageMap;
    }
    /**
     * Кеш переведённых строк локализации.
     * <p>
     * Ключ — строковый идентификатор слова или фразы,
     * значение — соответствующий перевод на текущем языке.
     * </p>
     *
     * <h3>Версия</h3>
     * <ul>
     *   <li>Введён в версии: 1.3</li>
     *   <li>Текущая версия реализации: 1.4.4</li>
     * </ul>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * locale_map.put("hello", "Привет");
     * String greeting = locale_map.get("hello");
     * }</pre>
     */
    private Map<String, String> locale_map = new HashMap<>();
    /**
     * Определяет язык локализации на основе системной страны пользователя.
     * <p>
     * При первом вызове проверяет наличие {@link #availableLanguage}, загружает его при необходимости.
     * Если системное свойство {@code user.country} присутствует и соответствует ключу в доступных языках,
     * возвращает соответствующую локаль. Иначе возвращает английскую локаль {@code "en"}.
     * </p>
     *
     * <h3>Версия</h3>
     * <ul>
     *   <li>Введён в версии: 1.4</li>
     *   <li>Текущая версия реализации: 1.4.4</li>
     * </ul>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * String locale = identifyLocale();
     * System.out.println("Определённый язык локализации: " + locale);
     * }</pre>
     *
     * <h3>Особенности</h3>
     * <ul>
     *   <li>Использует системное свойство {@code user.country}.</li>
     *   <li>Обновляет поле {@link #lang} выбранным языком.</li>
     *   <li>Возвращает английский язык по умолчанию, если не найдено значения для страны.</li>
     * </ul>
     *
     * @return код локали для использования в приложении
     */
    public String identifyLocale() {
        String userCountry = System.getProperty("user.country");

        if(availableLanguage == null) {
            availableLanguage = getAvailableLanguage();
        }

        if(userCountry != null) {
            return lang = availableLanguage.get(userCountry.toLowerCase());
        }

        return availableLanguage.get("en");
    }
    /**
     * Возвращает перевод строки по ключу.
     * <p>
     * При первом обращении к ключу ищет перевод в соответствующем файле локализации,
     * используя {@link FileManager}, и сохраняет результат в {@link #locale_map}.
     * Если перевод не найден, возвращает значение {@code ifResultIsNull}.
     * </p>
     *
     * <h3>Версия</h3>
     * <ul>
     *   <li>Введён в версии: 1.4</li>
     *   <li>Текущая версия реализации: 1.4.4</li>
     * </ul>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * String helloLocalized = getLocalizationString("hello", "Hello");
     * System.out.println(helloLocalized);
     * }</pre>
     *
     * <h3>Особенности</h3>
     * <ul>
     *   <li>Кеширует переводы в памяти для ускорения повторного доступа.</li>
     *   <li>Использует файл локализации, соответствующий текущему языку {@link #lang}.</li>
     *   <li>Обрабатывает ситуацию с пустым или отсутствующим кешем.</li>
     * </ul>
     *
     * @param word ключ переводимой строки
     * @param ifResultIsNull значение по умолчанию, если перевод не найден
     * @return переведённая строка или значение по умолчанию
     */
    public String getLocalizationString(String word, String ifResultIsNull) {
        if(locale_map == null) {
            locale_map = new HashMap<>();
        }

        if (!locale_map.containsKey(word)) {
            putLocale(word, ifResultIsNull);
        }

        return locale_map.get(word);
    }

    protected void putLocale(String word, String ifResultIsNull) {
        locale_map.put(word, fileManager.splitDataWithSpaces(
                fileManager.findFirstParam(
                        Path.of(resourceLanguageFile + File.separator + lang + ".locale"),
                        ifResultIsNull,
                        t -> t.contains(word)),
                ":")
        );
    }

    public Map<String, String> localeMap() {
        return locale_map;
    }
}
