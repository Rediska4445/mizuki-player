package rf.ebanina.File.Configuration;

import rf.ebanina.File.FileManager;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.ebanina.Music;
import rf.ebanina.utils.loggining.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;

/**
 * Основной класс для управления конфигурационными параметрами приложения.
 * <p>
 * Данный класс отвечает за чтение, кэширование и инициализацию конфигурационных
 * параметров из файлов, а также предоставляет удобные методы для получения
 * значений различных типов (строки, числа, булевы значения и т.д.).
 * </p>
 * <p>
 * Конфигурация загружается из указанного пути {@link #settingsDirectoryPath}, с использованием
 * вспомогательных классов {@link rf.ebanina.File.FileManager} и {@link ResourceManager}.
 * Поддерживается инициализация полей объектов через рефлексию с аннотацией {@link ConfigurableField}.
 * </p>
 * <p>
 * Также реализован вспомогательный вложенный класс {@link fxmlConverter}, который
 * преобразует конфигурационные данные в структуру FXML для удобства редактирования в графическом интерфейсе.
 * </p>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * // Получение экземпляра ConfigurationManager
 * ConfigurationManager config = ConfigurationManager.instance;
 *
 * // Инициализация аннотированных полей класса/объекта из конфигурации
 * config.initializeVariables(MySettings.class);
 *
 * // Получение параметров конфигурации
 * String userName = config.getItem("user.name", "defaultUser");
 * int maxThreads = config.getIntItem("thread.max", 10);
 * boolean debugMode = config.getBooleanItem("debug.enable", false);
 *
 * // Конвертация конфигурации в FXML
 * fxmlConverter.convert();
 * }</pre>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li>Поддержка различных типов данных для конфигурационных параметров с возможностью указания значений по умолчанию.</li>
 *   <li>Автоматическая инициализация полей классов с помощью аннотаций и рефлексии.</li>
 *   <li>Кэширование параметров в {@link #configurationMap} для быстрого доступа.</li>
 *   <li>Встроенный парсер конфигурационных файлов с поддержкой мультистрочных комментариев и секций.</li>
 *   <li>Генерация FXML файла для UI на основе конфигурации с сохранением описаний и tooltip’ов.</li>
 *   <li>Класс является синглтоном и не предназначен для наследования.</li>
 * </ul>
 *
 * @author Ebanina Std
 * @version 0.1.4.6
 * @since 0.1.1.0
 * @see ConfigurableField
 * @see FileManager
 * @see ResourceManager
 * @see LocalizationManager
 * @see fxmlConverter
 */
@logging(tag = "Configuration Manager")
public class ConfigurationManager
{
    /**
     * Путь к файлу настроек, используемый для загрузки конфигурации.
     * <p>
     * Определён на основе ресурса с ключом "configPath" из {@link ResourceManager}.
     * После инициализации предоставляет удобный доступ к файлу с конфигурацией.
     * </p>
     * <p>
     * Используется всеми методами класса для чтения и сохранения параметров.
     * </p>
     */
    private final Path settingsDirectoryPath;

    /**
     * Абсолютный путь к директории с файлами горячих клавиш.
     * <p>
     * Получается из ресурса с ключом "hotkeys" из {@link ResourceManager} и конвертируется в абсолютный путь.
     * Используется для поиска и загрузки файлов с определёнными настройками горячих клавиш.
     * </p>
     */
    public final Path hotKeysDirectoryPath;

    private final Path directoryPath;

    /**
     * Синглтон-экземпляр класса ConfigurationManager.
     * <p>
     * Обеспечивает глобальный доступ к настройкам приложения через единый объект.
     * Инстанцируется сразу при загрузке класса.
     * </p>
     */
    public static ConfigurationManager instance = new ConfigurationManager(
            Path.of("config", "settings.cfg"),
            Path.of("config", "bindings"),
            Path.of("config")
    );

    /**
     * Кэш конфигурационных значений.
     * <p>
     * Представлен в виде отображения ключей на строки.
     * При первом запросе параметра значение загружается из файла и кэшируется для быстрого доступа в дальнейшем.
     * </p>
     */
    private final Map<String, String> configurationMap = new HashMap<>();

    /**
     * Возвращает карту конфигурационных параметров.
     * <p>
     * Предоставляет кэшированное отображение ключей и значений конфигурации,
     * позволяя быстро получать доступ к загруженным параметрам.
     * </p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * Map<String, String> configMap = ConfigurationManager.instance.getConfigurationMap();
     * String value = configMap.get("someKey");
     * }</pre>
     *
     * @return {@code Map<String, String>} с текущими конфигурационными параметрами
     */
    public Map<String, String> getConfigurationMap() {
        return configurationMap;
    }

    /**
     * Конструктор ConfigurationManager.
     * <p>
     * Создает новый экземпляр менеджера конфигурации без предварительной загрузки.
     * Загрузка параметров происходит по мере необходимости.
     * </p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * ConfigurationManager manager = new ConfigurationManager();
     * }</pre>
     */
    public ConfigurationManager(final Path settingsDirectoryPath, final Path hotKeysDirectoryPath, Path directoryPath) {
        this.settingsDirectoryPath = settingsDirectoryPath;
        this.hotKeysDirectoryPath = hotKeysDirectoryPath;
        this.directoryPath = directoryPath;
    }

    public Path getDirectoryPath() {
        return directoryPath;
    }

    /**
     * Инициализирует переменные статических или нестатических классов,
     * помеченные аннотацией {@link ConfigurableField}.
     * <p>
     * Метод перебирает поля данных классов или объектов, извлекает
     * из конфигурации значения по ключам из аннотаций и устанавливает значения полей
     * с приведением к корректному типу через рефлексию.
     * </p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * ConfigurationManager.instance.initializeVariables(MyClass.class);
     * }</pre>
     *
     * @param classes массив классов для инициализации их помеченных полей
     */
    public void initializeVariables(Class<?>... classes) {
        for(Class<?> clazz : classes) {
            initializeVariables(clazz, null);
        }
    }
    /**
     * Инициализирует переменные конкретного класса или объекта,
     * помеченные аннотацией {@link ConfigurableField}.
     * <p>
     * Для каждого поля класса метод проверяет наличие аннотации,
     * получает ключ, извлекает значение из конфигурации,
     * преобразует значение к типу поля и устанавливает через рефлексию.
     * </p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * MyConfigObject instance = new MyConfigObject();
     * ConfigurationManager.instance.initializeVariables(MyConfigObject.class, instance);
     * }</pre>
     *
     * @param clazz    класс с аннотированными полями для инициализации
     * @param instance объект, для которого устанавливаются значения;
     *                 если {@code null}, инициализируются статические поля класса
     */
    public void initializeVariables(Class<?> clazz, Object instance) {
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(ConfigurableField.class)) {
                ConfigurableField annotation = field.getAnnotation(ConfigurableField.class);

                String key = annotation.key();
                String ifNull = annotation.ifNull();

                if (key.equalsIgnoreCase("null_field")) {
                    key = field.getName();
                }

                String stringValue = FileManager.instance.splitData(FileManager.instance.findFirstParam(key, settingsDirectoryPath, ifNull));

                field.setAccessible(true);

                try {
                    Class<?> type = field.getType();

                    if (type.equals(String.class)) {
                        field.set(instance, stringValue);
                    } else if (type.equals(int.class)) {
                        field.setInt(instance, Integer.parseInt(stringValue));
                    } else if (type.equals(long.class)) {
                        field.setLong(instance, Long.parseLong(stringValue));
                    } else if (type.equals(boolean.class)) {
                        field.setBoolean(instance, Boolean.parseBoolean(stringValue));
                    } else if (type.equals(double.class)) {
                        field.setDouble(instance, Double.parseDouble(stringValue));
                    } else if (type.equals(float.class)) {
                        field.setFloat(instance, Float.parseFloat(stringValue));
                    } else if (type.equals(short.class)) {
                        field.setShort(instance, Short.parseShort(stringValue));
                    } else if (type.equals(byte.class)) {
                        field.setByte(instance, Byte.parseByte(stringValue));
                    } else if (type.equals(char.class)) {
                        if (!stringValue.isEmpty()) {
                            field.setChar(instance, stringValue.charAt(0));
                        }
                    } else {
                        Music.mainLogger.println("Unknown type of field: ", type);
                    }
                } catch (IllegalAccessException | NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Возвращает значение конфигурационного параметра по ключу с использованием кэша.
     * <p>
     * Если значение с указанным ключом ещё не загружено, оно извлекается из конфигурационного файла,
     * сохраняется в кэш и возвращается вызывающему.
     * </p>
     * <p>
     * Позволяет экономить время при повторных запросах конфигурационных параметров.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * String userName = ConfigurationManager.instance.getItem("user.name", "defaultUser");
     * }</pre>
     *
     * @param key ключ конфигурационного параметра
     * @param e исключение если параметра нет
     * @see #configurationMap
     * @see #settingsDirectoryPath
     * @return значение параметра в виде строки
     */
    public String getItem(String key, Exception e) {
        if(!configurationMap.containsKey(key)) {
            configurationMap.put(key, FileManager.instance.splitData(FileManager.instance.findFirstParam(key, settingsDirectoryPath, e)));
        }

        return configurationMap.get(key);
    }

    /**
     * Возвращает значение конфигурационного параметра по ключу с использованием кэша.
     * <p>
     * Если значение с указанным ключом ещё не загружено, оно извлекается из конфигурационного файла,
     * сохраняется в кэш и возвращается вызывающему.
     * </p>
     * <p>
     * Позволяет экономить время при повторных запросах конфигурационных параметров.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * String userName = ConfigurationManager.instance.getItem("user.name", "defaultUser");
     * }</pre>
     *
     * @param key ключ конфигурационного параметра
     * @param ifNull значение по умолчанию, если параметр отсутствует в конфигурации
     * @see #configurationMap
     * @see #settingsDirectoryPath
     * @return значение параметра в виде строки
     */
    public String getItem(String key, String ifNull) {
        if(!configurationMap.containsKey(key)) {
            configurationMap.put(key, FileManager.instance.splitData(FileManager.instance.findFirstParam(key, settingsDirectoryPath, ifNull)));
        }

        return configurationMap.get(key);
    }
    /**
     * Возвращает значение конфигурационного параметра с использованием обобщенного типа.
     * <p>
     * Этот метод позволяет получить параметр из файла конфигурации, преобразуя его в указанный тип.
     * Если значение не удается преобразовать или отсутствует, возвращается значение по умолчанию.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * int maxConnections = configurationManager.getItem(Integer.class, "max.connections", 100);
     * }</pre>
     *
     * @param type класс типа, в который необходимо преобразовать значение
     * @param key ключ конфигурационного параметра
     * @param ifNull значение по умолчанию, если параметр отсутствует или не удалось преобразовать
     * @param <T> тип возвращаемого значения
     * @return значение параметра преобразованное к типу <T>, или {@code ifNull} при ошибке
     *
     * @see #getItem(String, String) для получения строкового значения с кэшированием
     * @see #getItem(Class, String, Object) для получения значения с конкретным типом
     */
    public <T> T getItem(Class<T> type, String key, T ifNull) {
        String value = FileManager.instance.findFirstParam(key, settingsDirectoryPath, ifNull.toString());

        if (value == null)
            return ifNull;

        try {
            if (type == String.class) {
                return type.cast(value);
            } else if (type == Integer.class) {
                return type.cast(Integer.parseInt(value));
            } else if (type == Float.class) {
                return type.cast(Float.parseFloat(value));
            } else if (type == Double.class) {
                return type.cast(Double.parseDouble(value));
            } else if (type == Boolean.class) {
                return type.cast(Boolean.parseBoolean(value));
            } else if (type == Long.class) {
                return type.cast(Long.parseLong(value));
            }
        } catch (Exception e) {
            return ifNull;
        }

        return ifNull;
    }

    /**
     * Возвращает значение конфигурационного параметра, загруженное напрямую без кэширования.
     * <p>
     * Значение извлекается из конфигурационного файла при каждом вызове, без использования
     * кэшированной копии.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * String currentValue = ConfigurationManager.instance.getNotCachedItem("app.version", "1.0");
     * }</pre>
     *
     * @implNote Этот метод вызывает {@link #getItem(Class, String, Object)},
     *           который в свою очередь использует определение типа.
     *
     * @param key ключ параметра конфигурации
     * @param ifNull значение по умолчанию, применяемое если параметр отсутствует
     * @return значение параметра как строка
     */
    public String getNotCachedItem(String key, String ifNull) {
        return getItem(String.class, key, ifNull);
    }

    /**
     * Возвращает значение конфигурационного параметра как int с кэшированием.
     *
     * @param key ключ параметра
     * @param ifNull значение по умолчанию в строковом виде
     * @return значение параметра как int
     */
    public int getIntItem(String key, String ifNull) {
        return Integer.parseInt(getItem(key, ifNull));
    }

    /**
     * Возвращает значение конфигурационного параметра как int с кэшированием.
     *
     * @param key ключ параметра
     * @param ifNull значение по умолчанию
     * @return значение параметра как int
     */
    public int getIntItem(String key, int ifNull) {
        return getItem(int.class, key, ifNull);
    }

    /**
     * Возвращает значение конфигурационного параметра как double с кэшированием.
     *
     * @param key ключ параметра
     * @param ifNull значение по умолчанию в строковом виде
     * @return значение параметра как double
     */
    public double getDoubleItem(String key, String ifNull) {
        return Double.parseDouble(getItem(key, ifNull));
    }

    /**
     * Возвращает значение конфигурационного параметра как boolean с кэшированием.
     *
     * <h3>
     *     Не путать с {@link ConfigurationManager#getBooleanItem(String, boolean)}!
     * </h3>
     *
     * Этот метод берёт из карты значение, и парсит его с помощью {@link Boolean#parseBoolean(String)}, не проверяя на ошибки
     *
     * @param key ключ параметра
     * @param ifNull значение по умолчанию в строковом виде
     * @return значение параметра как boolean
     */
    public boolean getBooleanItem(String key, String ifNull) {
        return Boolean.parseBoolean(getItem(key, ifNull));
    }

    /**
     * Возвращает значение конфигурационного параметра как boolean с кэшированием.
     *
     * <h3>
     *     Не путать с {@link ConfigurationManager#getBooleanItem(String, String)}!
     * </h3>
     *
     * Этот метод берёт из карты значение с помощью {@link ConfigurationManager#getItem(Class, String, Object)}, которое определяется в методе
     * @param key ключ параметра
     * @param ifNull значение по умолчанию
     * @return значение параметра как boolean
     */
    public boolean getBooleanItem(String key, boolean ifNull) {
        return getItem(boolean.class, key, ifNull);
    }

    /**
     * Возвращает строковое описание для всплывающей подсказки, связанной с ключом конфигурации.
     *
     * @param key ключ конфигурационного параметра
     * @return строка с описанием и локализацией
     */
    public String getTooltipString(String key) {
        return key + " - " + getLocaleString("configuration_is_zero", "is zero");
    }

    public Path getSettingsDirectoryPath() {
        return settingsDirectoryPath;
    }

    /**
     * <h1>fxmlConverter</h1>
     * <h1>Legacy Code</h1>
     * Вспомогательный класс для преобразования конфигурационных данных в FXML формат.
     * <p>
     * Состоит из двух вложенных классов: {@link ConfigParser} для парсинга конфигурационного
     * файла и {@link FxmlGenerator} для создания FXML-документа на основе парсинга.
     * </p>
     * <p>
     * Позволяет автоматизировать генерацию пользовательского интерфейса для настройки приложения
     * на основании конфигурационных данных.
     * </p>
     *
     * <h2>ConfigParser</h2>
     * <p>
     * Анализирует конфигурационный файл, выделяя секции и пары ключ-значение, а также комментарии к ним.
     * Поддерживает мультистрочные комментарии и автоматически группирует ключи по секциям.
     * </p>
     * <p>
     * Использует регулярные выражения для идентификации секций и параметров.
     * </p>
     * <h3>Пример использования</h3>
     * <pre>{@code
     * ConfigParser parser = new ConfigParser();
     * parser.parse(Paths.get("config.ini"));
     * Map<String, LinkedHashMap<String, String>> sections = parser.getSections();
     * Map<String, LinkedHashMap<String, String>> annotations = parser.getAnnotations();
     * }</pre>
     *
     * <h2>FxmlGenerator</h2>
     * <p>
     * Принимает результат работы {@link ConfigParser} и генерирует FXML файл для интерфейса настройки.
     * Формирует вкладки, поля ввода и подсказки на основе данных и комментариев.
     * </p>
     * <p>
     * Осуществляет экранирование XML-специфичных символов и формирует структуру UI.
     * </p>
     * <h3>Пример использования</h3>
     * <pre>{@code
     * FxmlGenerator generator = new FxmlGenerator();
     * try (Writer writer = Files.newBufferedWriter(Paths.get("config.fxml"))) {
     *     generator.generateFxml(sections, annotations, writer);
     * }
     * }</pre>
     *
     * <h2>Особенности</h2>
     * <ul>
     *   <li>Поддержка комментариев и секций в конфигурации.</li>
     *   <li>Создание динамического пользовательского интерфейса на базе конфигурации.</li>
     *   <li>Сравнение структуры FXML и конфигурации для предотвращения ненужной генерации.</li>
     *   <li>Метод {@link #convert()} автоматически обновляет FXML при изменении конфигурации.</li>
     * </ul>
     *
     * @author Ebanina Std
     * @version 0.1.4.6
     * @since 0.1.3.5
     * @see ConfigurationManager
     */
    public static class fxmlConverter {
        /**
         * Парсер конфигурационного файла, извлекающий данные и комментарии по секциям.
         * <p>
         * Этот класс анализирует конфигурационный файл построчно, выделяя отдельные секции,
         * обозначаемые специальным шаблоном {@code <-sectionName->}, и параметрические ключи с соответствующими значениями.
         * При обработке поддерживает мультистрочные аннотации-комментарии, которые временно накапливаются
         * и ассоциируются с последующим параметром.
         * </p>
         * <p>
         * Для распознавания секций и параметров используются регулярные выражения, обеспечивающие гибкость структуры конфигурации.
         * Значения параметров дополнительно очищаются от внешних кавычек для корректного использования в дальнейшем.
         * </p>
         * <p>
         * Результатом работы парсера являются:
         * <ul>
         *   <li>Структурированный набор секций с параметрами, представленным как {@code Map<String, LinkedHashMap<String, String>>}, сохраняющий порядок</li>
         *   <li>Параллельная структура комментариев к параметрам с таким же порядком, позволяющая создавать расширенную документацию или подсказки</li>
         * </ul>
         * </p>
         *
         * <h3>Особенности</h3>
         * <ul>
         *   <li>Обработка мультистрочных комментариев, начинающихся с {@code !commentary="}</li>
         *   <li>Отсечение пустых строк и игнорирование строк вне определённых секций</li>
         *   <li>Поддержка почему и граничных случаев разделения секций</li>
         *   <li>Чёткая разграниченность структур данных на параметры и комментарии</li>
         * </ul>
         *
         * <h3>Пример использования</h3>
         * <pre>{@code
         * ConfigParser parser = new ConfigParser();
         * parser.parse(Paths.get("config.ini"));
         * Map<String, LinkedHashMap<String, String>> sections = parser.getSections();
         * Map<String, LinkedHashMap<String, String>> annotations = parser.getAnnotations();
         * }</pre>
         */
        protected static class ConfigParser {
            /**
             * Карта для хранения секций конфигурационного файла.
             * <p>
             * Используется {@link LinkedHashMap} для сохранения порядка добавления секций и параметров.
             * Ключ — имя секции, значение — карта параметров в этой секции.
             * </p>
             */
            private final Map<String, LinkedHashMap<String, String>> sections = new LinkedHashMap<>();

            /**
             * Карта для хранения аннотаций (комментариев) к параметрам.
             * <p>
             * Структура аналогична {@link #sections} — ключи секций совпадают,
             * значения представляют собой карты комментариев к параметрам.
             * Позволяет хранить дополнительную информацию о каждом параметре.
             * </p>
             */
            private final Map<String, LinkedHashMap<String, String>> annotations = new LinkedHashMap<>();

            /**
             * Регулярное выражение для поиска заголовков секций конфигурационного файла.
             * <p>
             * Формат секции: строка вида {@code <-sectionName->}.
             * Группа захвата выделяет имя секции.
             * </p>
             */
            private static final Pattern SECTION_PATTERN = Pattern.compile("<-+(.+?)-+>");

            /**
             * Регулярное выражение для разбора строк параметров.
             * <p>
             * Формат: {@code ключ = значение ;} с возможными пробелами.
             * Ключ содержит латинские буквы, цифры и символ подчеркивания.
             * Значение захватывается до символа точки с запятой.
             * </p>
             */
            private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^\\s*([a-zA-Z0-9_]+)\\s*=\\s*(.*);\\s*$");
            /**
             * Анализирует конфигурационный файл по указанному пути и заполняет внутренние структуры данными.
             * <p>
             * Метод читает все строки файла, пропуская пустые. Определяет текущую секцию по шаблону,
             * создаёт новые записи для параметров и комментариев.
             * Поддерживает мультистрочные комментарии, которые накапливает до закрывающей кавычки.
             * Парсит строки с параметрами формата {@code ключ = значение ;}, очищая кавычки вокруг значений.
             * </p>
             * <p>
             * При отсутствии текущей секции параметры игнорируются. Комментарии сохраняются временно
             * и приписываются к следующему параметру.
             * </p>
             *
             * <h3>Особенности</h3>
             * <ul>
             *   <li>Использование регулярных выражений для определения секций и параметров.</li>
             *   <li>Поддержка комментариев разных форматов, в том числе мультистрочных.</li>
             *   <li>Сохранение порядка секций и параметров через {@link LinkedHashMap}.</li>
             * </ul>
             *
             * <h3>Пример использования</h3>
             * <pre>{@code
             * ConfigParser parser = new ConfigParser();
             * parser.parse(Paths.get("config.ini"));
             * Map<String, LinkedHashMap<String, String>> sections = parser.getSections();
             * Map<String, LinkedHashMap<String, String>> annotations = parser.getAnnotations();
             * }</pre>
             */
            public void parse(Path configFile) throws IOException {
                List<String> lines = Files.readAllLines(configFile);
                String currentSection = null;

                Map<String, String> tempAnnotations = new HashMap<>(); // временное хранение аннотаций по ключу
                StringBuilder multiLineBuilder = new StringBuilder();
                boolean isMultiLineAnnotation = false;

                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    Matcher sectionMatcher = SECTION_PATTERN.matcher(line);
                    if (sectionMatcher.matches()) {
                        currentSection = sectionMatcher.group(1).trim();
                        sections.put(currentSection, new LinkedHashMap<>());
                        annotations.put(currentSection, new LinkedHashMap<>());
                        tempAnnotations.clear();
                        isMultiLineAnnotation = false;
                        multiLineBuilder.setLength(0);
                        continue;
                    }

                    if (currentSection == null) continue;

                    if (isMultiLineAnnotation) {
                        if (line.endsWith("\"")) {
                            multiLineBuilder.append(line, 0, line.length() - 1);
                            tempAnnotations.put("commentary", multiLineBuilder.toString().trim());
                            isMultiLineAnnotation = false;
                            multiLineBuilder.setLength(0);
                        } else {
                            multiLineBuilder.append(line).append("\n");
                        }
                        continue;
                    }

                    if (line.startsWith("!commentary=\"")) {
                        if (line.endsWith("\"") && !line.equals("!commentary=\"")) {
                            String annoValue = line.substring("!commentary=\"".length(), line.length() - 1);
                            tempAnnotations.put("commentary", annoValue.trim());
                        } else {
                            String afterEquals = line.substring("!commentary=\"".length());
                            multiLineBuilder.append(afterEquals).append("\n");
                            isMultiLineAnnotation = true;
                        }
                        continue;
                    }

                    // Парсим параметр
                    Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(line);
                    if (kvMatcher.matches()) {
                        String key = kvMatcher.group(1).trim();
                        String value = kvMatcher.group(2).trim();
                        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                            value = value.substring(1, value.length() - 1);
                        }
                        sections.get(currentSection).put(key, value);

                        annotations.get(currentSection).put(key, tempAnnotations.getOrDefault("commentary", ""));
                        tempAnnotations.clear();
                    }
                }
            }
            /**
             * Возвращает карты секций и параметров, полученные после парсинга.
             *
             * @return карта секций с параметрами конфигурации
             */
            public Map<String, LinkedHashMap<String, String>> getSections () {
                return sections;
            }

            /**
             * Возвращает карты аннотаций (комментариев) к параметрам после парсинга.
             *
             * @return карта секций с комментариями к параметрам
             */
            public Map<String, LinkedHashMap<String, String>> getAnnotations () {
                return annotations;
            }
        }
        /**
         * Класс, отвечающий за генерацию FXML документа на основе парсенных конфигурационных данных.
         * <p>
         * Основная задача — сформировать XML-разметку пользовательского интерфейса,
         * содержащую вкладки, поля ввода, чекбоксы и подсказки, соответствующие параметрам конфигурации.
         * </p>
         * <p>
         * При генерации используются данные секций и комментариев из парсера, а также утилиты для
         * локализации и безопасного экранирования XML-символов.
         * </p>
         *
         * <h3>Особенности</h3>
         * <ul>
         *   <li>Вложенные структуры FXML создаются динамически для каждой секции и параметра.</li>
         *   <li>Поддержка чекбоксов для булевых значений и текстовых полей для остальных типов.</li>
         *   <li>Использование подсказок, основанных на комментариях и локализованных строках.</li>
         *   <li>Экранирование спецсимволов XML для предотвращения ошибок парсинга и отображения.</li>
         *   <li>Автоматическое подключение необходимых JavaFX импортов и атрибутов.</li>
         * </ul>
         *
         * <h3>Пример использования</h3>
         * <pre>{@code
         * FxmlGenerator generator = new FxmlGenerator();
         * try (Writer writer = Files.newBufferedWriter(Paths.get("config.fxml"))) {
         *     generator.generateFxml(sections, annotations, writer);
         * }
         * }</pre>
         */
        private static class FxmlGenerator {
            /**
             * Генерирует FXML-документ, представляющий пользовательский интерфейс настройки.
             * <p>
             * Использует секции и параметры из конфигурационного файла, а также комментарии,
             * чтобы создать структуру с вкладками и элементами управления для каждого параметра.
             * </p>
             * <p>
             * Для каждого параметра создаётся VBox с Label (отображающим имя и ключ), подсказкой (tooltip),
             * и соответствующим элементом управления: CheckBox для булевых значений или TextField для остальных.
             * Также создаётся TextArea с подробной информацией из комментариев, которая скрыта по умолчанию.
             * </p>
             * <p>
             * Все элементы генерируются с учётом правильного экранирования XML-символов для безопасности.
             * В документ добавляются необходимые импорты JavaFX и базовая стилизация.
             * </p>
             *
             * <h3>Особенности</h3>
             * <ul>
             *   <li>Динамическое создание вкладок по секциям конфигурации.</li>
             *   <li>Отдельный ScrollPane для каждой вкладки с прокруткой содержимого.</li>
             *   <li>Интерактивные обработчики событий мыши для показа и скрытия детальной информации.</li>
             *   <li>Продуманное использование CSS атрибутов для внешнего вида элементов.</li>
             * </ul>
             *
             * <h3>Пример использования</h3>
             * <pre>{@code
             * FxmlGenerator generator = new FxmlGenerator();
             * try (Writer writer = Files.newBufferedWriter(Paths.get("config.fxml"))) {
             *     generator.generateFxml(sections, annotations, writer);
             * }
             * }</pre>
             *
             * @param sections карта секций с параметрами конфигурации
             * @param annotations карта секций с комментариями к параметрам
             * @param writer поток вывода для записи FXML-документа
             * @throws IOException при ошибках ввода-вывода
             */
            public void generateFxml(Map<String, LinkedHashMap<String, String>> sections,
                                     Map<String, LinkedHashMap<String, String>> annotations,
                                     Writer writer) throws IOException {
                PrintWriter out = new PrintWriter(writer);

                out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                out.println();
                out.println("<?import javafx.scene.control.*?>");
                out.println("<?import javafx.scene.layout.*?>");
                out.println("<?import javafx.geometry.Insets?>");
                out.println();

                out.println("<AnchorPane xmlns:fx=\"http://javafx.com/fxml\" prefWidth=\"600\" prefHeight=\"600\" fx:controller=\"rf.ebanina.File.Configuration.GuiController\"");
                out.println("            style=\"-fx-background-color: #f7f7f7;\">");
                out.println("    <children>");
                out.println("        <TabPane fx:id=\"tabPane\" AnchorPane.topAnchor=\"10\" AnchorPane.leftAnchor=\"10\" AnchorPane.rightAnchor=\"10\" AnchorPane.bottomAnchor=\"50\">");
                out.println("            <tabs>");

                for (Map.Entry<String, LinkedHashMap<String, String>> sectionEntry : sections.entrySet()) {
                    String sectionName = sectionEntry.getKey();
                    LinkedHashMap<String, String> params = sectionEntry.getValue();
                    LinkedHashMap<String, String> sectionAnnotations = annotations.getOrDefault(sectionName, new LinkedHashMap<>());

                    out.printf("                <Tab text=\"%s\" closable=\"false\" style=\"-fx-text-base-color: #7092be;\">%n", escapeXml(sectionName));
                    out.println("                    <content>");
                    out.println("                        <ScrollPane fitToWidth=\"true\" fitToHeight=\"true\">");
                    out.println("                            <content>");
                    out.println("                                <VBox alignment=\"TOP_LEFT\" spacing=\"8\" style=\"-fx-padding: 15; -fx-background-color: #f7f7f7;\">");

                    for (Map.Entry<String, String> param : params.entrySet()) {
                        String key = param.getKey();
                        String value = param.getValue();

                        String humanName = rf.ebanina.File.Localization.LocalizationManager.getLocaleString(key, key);
                        String displayName = String.format("%s (%s)", humanName, key);
                        String tooltip = ConfigurationManager.instance.getTooltipString(key);

                        out.printf("                                    <VBox fx:id=\"%sVBox\" spacing=\"2\" alignment=\"CENTER_LEFT\" style=\"-fx-padding: 6 0 6 0;\" onMouseEntered=\"#showDetail\" onMouseExited=\"#hideDetail\">%n", escapeXml(key));

                        out.println("                                        <HBox spacing=\"10\" alignment=\"CENTER_LEFT\">");

                        out.printf("                                            <Label text=\"%s\" style=\"-fx-text-fill: #7092be; -fx-font-weight: bold;\">%n", escapeXml(displayName));
                        out.printf("                                                <tooltip><Tooltip text=\"%s\"/></tooltip>%n", escapeXml(tooltip));
                        out.println("                                            </Label>");

                        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                            boolean selected = Boolean.parseBoolean(value);
                            out.printf("                                            <CheckBox fx:id=\"%s\" mnemonicParsing=\"false\" selected=\"%s\" />%n", escapeXml(key), selected);
                        } else {
                            out.printf("                                            <TextField fx:id=\"%s\" text=\"%s\" style=\"-fx-background-color: white; -fx-border-color: #7092be; -fx-text-fill: black; -fx-background-radius: 5; -fx-border-radius: 5;\" />%n", escapeXml(key), escapeXml(value));
                        }

                        out.println("        </HBox>");

                        String annotationText = sectionAnnotations.getOrDefault(key, "Подробная информация отсутствует");
                        annotationText = annotationText.isEmpty() ? "Подробная информация отсутствует" : annotationText;
                        
                        out.printf("        <TextArea fx:id=\"%sDetail\" text=\"%s\" visible=\"false\" managed=\"false\" editable=\"false\" wrapText=\"true\" style=\"-fx-font-style: italic; -fx-text-fill: gray; -fx-padding: 0 0 0 20;\" prefRowCount=\"3\" />%n",
                                escapeXml(key), escapeXml(annotationText));

                        out.println("    </VBox>");
                    }

                    out.println("                                </VBox>");
                    out.println("                            </content>");
                    out.println("                        </ScrollPane>");
                    out.println("                    </content>");
                    out.println("                </Tab>");
                }

                out.println("            </tabs>");
                out.println("        </TabPane>");

                out.println("        <Button fx:id=\"saveButton\" text=\"Сохранить\" AnchorPane.bottomAnchor=\"10\" AnchorPane.rightAnchor=\"10\" " +
                        "prefWidth=\"100\" prefHeight=\"30\" " +
                        "style=\"-fx-background-color: #7092be; -fx-text-fill: white; -fx-background-radius: 5; -fx-font-weight: bold;\" />");

                out.println("    </children>");
                out.println("</AnchorPane>");

                out.flush();
            }
            /**
             * Выполняет безопасное экранирование специальных символов для использования в XML.
             * <p>
             * Заменяет символы {@code &}, {@code <}, {@code >}, {@code "}, {@code '} на их соответствующие
             * сущности {@code &amp;}, {@code &lt;}, {@code &gt;}, {@code &quot;}, {@code &apos;}.
             * Это необходимо для обеспечения корректного парсинга и отображения XML-документов,
             * предотвращая разбор этих символов как управляющих тегов или атрибутов.
             * </p>
             * <p>
             * Входная строка может быть {@code null}, в этом случае метод возвращает пустую строку.
             * </p>
             *
             * <h3>Особенности</h3>
             * <ul>
             *   <li>Обрабатываются наиболее распространённые специальные XML символы.</li>
             *   <li>Метод предназначен для гарантированного безопасного включения текста в XML.</li>
             *   <li>Не выполняет обратного преобразования (декодирования).</li>
             * </ul>
             *
             * <h3>Пример использования</h3>
             * <pre>{@code
             * String safeXmlText = escapeXml(userInput);
             * out.println("<Label text=\"" + safeXmlText + "\"/>");
             * }</pre>
             *
             * @param s исходная строка для экранирования
             * @return строка с заменёнными XML-специальными символами, безопасная для использования в XML
             */
            private String escapeXml(String s) {
                if (s == null)
                    return "";

                return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                        .replace("\"", "&quot;").replace("'", "&apos;");
            }
        }
        /**
         * Утилитарный класс для сравнения структуры конфигурационного файла и FXML.
         * <p>
         * Используется для определения необходимости генерации нового FXML-файла, если структура конфигурации изменилась.
         * </p>
         *
         * <h3>Метод parseFxmlStructure</h3>
         * <p>
         * Анализирует FXML-файл, извлекает названия вкладок и идентификаторы элементов внутри них.
         * Результат — карта: ключ — имя вкладки, значение — набор fx:id элементов.
         * </p>
         *
         * <h3>Метод isStructureEqual</h3>
         * <p>
         * Сравнивает ключи секций и наборы параметров между конфигурацией и FXML.
         * Возвращает {@code true}, если структура совпадает, {@code false} — при несоответствии.
         * </p>
         *
         * <h3>Использование</h3>
         * <pre>{@code
         * Path fxmlPath = Paths.get("config.fxml");
         * Map<String, Set<String>> fxmlStructure = ConfigFxmlComparator.parseFxmlStructure(fxmlPath);
         * boolean equal = ConfigFxmlComparator.isStructureEqual(configSections, fxmlStructure);
         * if (!equal) {
         *     // Генерируем новый FXML
         * }
         * }</pre>
         */
        private static class ConfigFxmlComparator {
            /**
             * Производит парсинг структуры существующего FXML-файла, извлекая разделы и идентификаторы элементов.
             * <p>
             * Сканирует строки файла, обнаруживает теги вкладок (Tab) и внутри них элементы с атрибутом fx:id.
             * Собирает эту структуру в виде карты, где ключ — имя секции (вкладки), значение — набор fx:id элементов.
             * </p>
             * <p>
             * Используется для сравнения актуальной структуры FXML с конфигурацией,
             * чтобы определить необходимость перегенерации интерфейса.
             * </p>
             *
             * @param fxmlFile путь к существующему FXML файлу
             * @return карта, где ключ — имя вкладки, значение — набор fx:id элементов в ней
             * @throws IOException при невозможности прочитать файл
             */
            public static Map<String, Set<String>> parseFxmlStructure(Path fxmlFile) throws IOException {
                Map<String, Set<String>> fxmlStructure = new LinkedHashMap<>();

                List<String> lines = Files.readAllLines(fxmlFile);
                String currentSection = null;
                Pattern tabPattern = Pattern.compile("<Tab\\s+[^>]*text=\"([^\"]+)\"[^>]*>");
                Pattern fxIdPattern = Pattern.compile("fx:id=\"([^\"]+)\"");

                for (String line : lines) {
                    line = line.trim();

                    Matcher tabMatcher = tabPattern.matcher(line);
                    if (tabMatcher.find()) {
                        currentSection = tabMatcher.group(1);
                        fxmlStructure.put(currentSection, new LinkedHashSet<>());
                        continue;
                    }

                    if (currentSection != null) {
                        Matcher fxIdMatcher = fxIdPattern.matcher(line);
                        while (fxIdMatcher.find()) {
                            String fxId = fxIdMatcher.group(1);
                            fxmlStructure.get(currentSection).add(fxId);
                        }
                    }

                    if (line.equals("</Tab>")) {
                        currentSection = null;
                    }
                }

                return fxmlStructure;
            }
            /**
             * Производит парсинг структуры существующего FXML-файла, извлекая разделы и идентификаторы элементов.
             * <p>
             * Сканирует строки файла, обнаруживает теги вкладок (Tab) и внутри них элементы с атрибутом fx:id.
             * Собирает эту структуру в виде карты, где ключ — имя секции (вкладки), значение — набор fx:id элементов.
             * </p>
             * <p>
             * Используется для сравнения актуальной структуры FXML с конфигурацией,
             * чтобы определить необходимость перегенерации интерфейса.
             * </p>
             *
             * @return карта, где ключ — имя вкладки, значение — набор fx:id элементов в ней
             */
            public static boolean isStructureEqual(Map<String, LinkedHashMap<String, String>> configSections,
                                                   Map<String, Set<String>> fxmlStructure) {
                if (!configSections.keySet().equals(fxmlStructure.keySet())) {
                    return false;
                }

                for (String section : configSections.keySet()) {
                    Set<String> configKeys = configSections.get(section).keySet();
                    Set<String> fxmlKeys = fxmlStructure.get(section);
                    if (!new HashSet<>(configKeys).equals(fxmlKeys)) {
                        return false;
                    }
                }

                return true;
            }
        }
        /**
         * Основной метод конвертации конфигурационного файла в FXML.
         * <p>
         * Загружает конфигурационный файл и парсит его с помощью {@link ConfigParser}.
         * Далее сравнивает текущую структуру с уже существующим FXML-файлом с помощью {@link ConfigFxmlComparator}.
         * Если структуры отличаются или FXML не существует, создает новый FXML файл через {@link FxmlGenerator}.
         * </p>
         * <p>
         * Позволяет автоматически поддерживать актуальный пользовательский интерфейс для конфигурации.
         * Обработка исключений ввода-вывода встроена для информирования о возможных ошибках на этапе преобразования.
         * </p>
         *
         * <h3>Особенности</h3>
         * <ul>
         *   <li>Путь используется из {@link ConfigurationManager#instance}.</li>
         *   <li>Логика проверки необходимости регенерации FXML для оптимизации производительности.</li>
         *   <li>Корректное закрытие потоков записи с помощью try-with-resources.</li>
         * </ul>
         *
         * <h3>Пример использования</h3>
         * <pre>{@code
         * fxmlConverter.convert();
         * }</pre>
         */
        public static void convert() {
            Path configFile = ConfigurationManager.instance.settingsDirectoryPath;
            Path outputFxml = Paths.get(ResourceManager.Instance.resourcesPaths.get("FXMLSettingsPath"));

            ConfigParser parser = new ConfigParser();
            FxmlGenerator generator = new FxmlGenerator();

            try {
                parser.parse(configFile);

                boolean needGenerate = true;

                if (Files.exists(outputFxml)) {
                    Map<String, Set<String>> fxmlStructure = ConfigFxmlComparator.parseFxmlStructure(outputFxml);
                    needGenerate = !ConfigFxmlComparator.isStructureEqual(parser.getSections(), fxmlStructure);
                }

                if (needGenerate) {
                    try (Writer writer = Files.newBufferedWriter(outputFxml)) {
                        generator.generateFxml(parser.getSections(), parser.getAnnotations(), writer);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}