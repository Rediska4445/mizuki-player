package rf.ebanina.File.Configuration;

import rf.ebanina.File.FileManager;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.ebanina.Music;
import rf.ebanina.utils.loggining.logging;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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
 * @version 0.1.4.7
 * @since 0.1.1.0
 * @see ConfigurableField
 * @see FileManager
 * @see ResourceManager
 * @see LocalizationManager
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

    public static ConfigurationManager defaultInstance() {
        return new ConfigurationManager(
                Path.of("config", "settings.cfg"),
                Path.of("config", "bindings"),
                Path.of("config")
        );
    }

    public static ConfigurationManager getInstance() {
        if(instance == null)
            return instance = defaultInstance();

        return instance;
    }

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

    public Path getSettingsDirectoryPath() {
        return settingsDirectoryPath;
    }
}