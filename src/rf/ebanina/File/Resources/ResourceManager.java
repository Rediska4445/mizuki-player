package rf.ebanina.File.Resources;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.FileManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * <h1>ResourceManager</h1>
 * Синглтон-класс для удобной работы с ресурсами JavaFX и других типов, определённых через properties-файл ресурсов.
 * <p>
 * {@code ResourceManager} отвечает за чтение путей к ресурсам из файла <code>resources.properties</code> и разрешение
 * переменных внутри значений путей в формате <code>${variable}</code>, а также корректную замену разделителей пути
 * с '/' на системно-зависимый <code>File.separator</code>.
 * </p>
 * <p>
 * Класс не загружает ресурсы напрямую в память — его задача предоставить механизмы получения корректного объекта
 * JavaFX (например, {@link javafx.scene.image.Image}, {@link javafx.scene.text.Font}, {@link javafx.scene.Parent} для FXML)
 * или других типов (например, {@link File}) по идентификатору ресурса.
 * </p>
 * <p>
 * Все ресурсы должны быть описаны в файле <code>resources.properties</code>, откуда при инициализации класса считываются пути.
 * Можно также загружать отдельные файлы по полному пути, обходя свойства.
 * </p>
 * <p>
 * Помимо базового функционала, предусмотрена поддержка следующих типов ресурсов:
 * <ul>
 *   <li>Изображения ({@code Image}): PNG, JPEG, GIF, ICO.</li>
 *   <li>Стили (CSS): строки с URL стилей.</li>
 *   <li>FXML: объект JavaFX {@code Parent} для загрузки интерфейсов.</li>
 *   <li>Шрифты (TTF, OTF).</li>
 *   <li>SVG: путь к файлу SVG.</li>
 *   <li>Файлы произвольного типа.</li>
 * </ul>
 * </p>
 * <p>
 * Особенности загрузки изображений — можно передавать параметры высоты, ширины, сохранения пропорций и сглаживания.
 * Параметры шрифта — размер. В загрузке остальных типов параметры не используются.
 * </p>
 *
 * <h2>Инициализация</h2>
 * <pre>{@code
 * // Единичный экземпляр класса
 * ResourceManager rm = ResourceManager.Instance;
 *
 * // По умолчанию загружает ресурсы из "res/resources.properties"
 * }</pre>
 *
 * <h2>Пример использования</h2>
 * <pre>{@code
 * // Загрузка иконки размером 64x64 с сохранением пропорций и сглаживанием
 * Image image = ResourceManager.Instance.loadImage("icon.main", 64, 64, true, true);
 *
 * // Загрузка CSS-стиля
 * String cssUrl = ResourceManager.Instance.loadStylesheet("style.main");
 *
 * // Загрузка FXML интерфейса
 * Parent root = ResourceManager.Instance.loadFXML("mainWindow");
 *
 * // Загрузка шрифта размером 14pt
 * Font font = ResourceManager.Instance.loadFont("font.roboto", 14);
 *
 * // Получение файла ресурса напрямую
 * File someFile = ResourceManager.Instance.loadResourcePathFile("data.file");
 * }</pre>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li>Класс не кеширует загруженные ресурсы — каждый вызов {@code loadResource} создаёт новый объект.</li>
 *   <li>Загружаемые ресурсы должны иметь корректные пути в properties-файле с возможными переменными в формате <code>${}</code>.</li>
 *   <li>В данном классе отсутствует логика обработки ошибок загрузки ресурсов за исключением логирования ошибок ввода-вывода.</li>
 *   <li>Для загрузки FXML используется {@link javafx.fxml.FXMLLoader}, причём метод {@code loadFXML} бросает исключение {@link IOException}.</li>
 *   <li>Статический объект {@link #Instance} реализует синглтон с публичным доступом.</li>
 *   <li>Поддержка расширений и дополнительных параметров через массив строк {@code extensions} в методе {@code loadResource}.</li>
 * </ul>
 *
 * @author Ebanina Std.
 * @version 0.1.4.4
 * @since 0.1.3.1
 * @see javafx.scene.image.Image
 * @see javafx.scene.text.Font
 * @see javafx.scene.Parent
 * @see javafx.fxml.FXMLLoader
 * @see java.util.Properties
 */
public class ResourceManager
    implements IResourceManager
{
    /**
     * Абсолютный путь к директории, содержащей бинарные библиотеки, необходимые для работы приложения.
     * <p>Данный путь используется для загрузки нативных модулей и расширений.</p>
     *
     * <p><b>Пример использования:</b></p>
     * <pre>{@code
     * System.out.println(ResourceManager.BIN_LIBRARIES_PATH);
     * }</pre>
     */
    public static String BIN_LIBRARIES_PATH = Path.of("libraries", "bin").toAbsolutePath().toString();
    /**
     * Карта ключей ресурсов к абсолютным путям их файлов с корректным разрешением переменных и системных разделителей.
     * <p>Хранит данные после загрузки из properties, чтобы быстро находить местоположение ресурса по его идентификатору.</p>
     * <p><b>Версия:</b> 0.1.4.0</p>
     *
     * <p><b>Пример заполнения:</b></p>
     * <pre>{@code
     * // после вызова loadResources("path/to/resources.properties")
     * // resourcesPaths может содержать:
     * // "icon.main" → "/absolute/path/to/images/main_icon.png"
     * // "font.roboto" → "/absolute/path/to/fonts/Roboto-Regular.ttf"
     * }</pre>
     */
    public Map<String, String> resourcesPaths = new HashMap<>();
    /**
     * Загружает ресурсы из properties-файла с поддержкой переменных и нормализацией путей.
     * <p>Метод читает ключ-значение, резолвит переменные вида <code>${var}</code> через рекурсивный подстановщик,
     * а также заменяет символы '/' на системный разделитель (File.separator).</p>
     * <p>Результаты заносятся в {@link #resourcesPaths}.</p>
     * <p><b>Версия:</b> 0.1.4.1</p>
     *
     * <p><b>Детали работы:</b></p>
     * <ul>
     *   <li>Если файл не найден, метод завершает работу без нарушения процесса (без исключений).</li>
     *   <li>Переменные в значениях разрешаются последовательно до полного раскрытия либо пока не встречается неизвестная переменная.</li>
     *   <li>Пути нормализуются с учётом платформенной специфики.</li>
     * </ul>
     *
     * <p><b>Назначение:</b> подготовить внутреннюю карту ресурсов для последующей быстрой и унифицированной загрузки.</p>
     *
     * <p><b>Пример использования:</b></p>
     * <pre>{@code
     * ResourceManager rm = ResourceManager.Instance;
     * rm.loadResources("config/resources.properties");
     * String path = rm.resourcesPaths.get("icon.main");
     * }</pre>
     *
     * @param filePath путь к properties-файлу (абсолютный или относительный)
     */
    public void loadResources(String filePath) {
        File file = new File(filePath);

        if(resourcesPaths != null) {
            resourcesPaths.clear();
        } else {
            resourcesPaths = new HashMap<>();
        }

        if (!file.exists()) {
            return;
        }

        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream(file.getAbsolutePath())) {
            properties.load(input);

            Map<String, String> rawProps = new HashMap<>();
            for (java.util.Map.Entry<Object, Object> entry : properties.entrySet()) {
                rawProps.put(
                        String.valueOf(entry.getKey()),
                        String.valueOf(entry.getValue())
                );
            }

            for (java.util.Map.Entry<Object, Object> entry : properties.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String value = String.valueOf(entry.getValue());

                String resolvedValue = resolveVariables(value, rawProps);
                resolvedValue = resolvedValue.replace("/", File.separator);

                resourcesPaths.put(key, resolvedValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Рекурсивно раскрывает переменные <code>${variable}</code> в строке, используя предоставленный набор ключ-значение.
     * <p>Подставляет значения переменных до тех пор, пока остаются нераскрытые вхождения или встретит неизвестную переменную.</p>
     * <p><b>Версия:</b> 0.1.4.2</p>
     *
     * <p><b>Детали:</b> метод поочерёдно ищет вхождения переменных, извлекает имя и подставляет значение из карты,
     * обновляя строку на каждом шаге.</p>
     *
     * <p><b>Для чего используется:</b> позволяет в properties-файлах использовать переменные, упрощая конфигурацию и поддержку.</p>
     *
     * <p><b>Пример:</b></p>
     * <pre>{@code
     * Map<String, String> props = Map.of(
     *   "theme", "dark",
     *   "path", "images/${theme}/icon.png"
     * );
     * String resolved = resolveVariables(props.get("path"), props);
     * // resolved -> "images/dark/icon.png"
     * }</pre>
     *
     * @param value строка с одной или несколькими переменными в формате ${varName}
     * @param props карта всех доступных переменных и их значений
     * @return строка с подставленными значениями вместо переменных
     */
    private String resolveVariables(String value, Map<String, String> props) {
        while (value.contains("${")) {
            int start = value.indexOf("${");
            int end = value.indexOf("}", start);

            if (end == -1)
                break;

            String varName = value.substring(start + 2, end);
            String varValue = props.get(varName);

            if (varValue == null) {
                break;
            }

            value = value.substring(0, start) + varValue + value.substring(end + 1);
        }

        return value;
    }

    public static ResourceManager Instance = new    ResourceManager(
            ConfigurationManager.instance.getItem("theme", "res/resources.properties").replace("/", File.separator)
    );

    /**
     * Универсальный метод для загрузки ресурса по типу и идентификатору с возможностью дополнительных настроек.
     * <p>Выполняет поиск пути ресурса в map, проверяет тип и валидирует расширение, после чего загружает и инициализирует нужный объект.</p>
     * <p><b>Версия:</b> 0.1.4.4</p>
     *
     * <p><b>Как работает:</b></p>
     * <ol>
     *   <li>По resourceId находит путь в {@link #resourcesPaths}.</li>
     *   <li>Проверяет расширение файла и сопоставляет с типом ресурса (IMAGE, FONT, etc.).</li>
     *   <li>Если проходит проверку, создаёт объект соответствующего класса, используя дополнительные параметры {@code extensions}.</li>
     *   <li>В случае ошибки или несоответствия возвращает {@code null}.</li>
     * </ol>
     *
     * <p><b>Для чего:</b> централизовать и унифицировать загрузку разных типов ресурсов одним методом.</p>
     *
     * <p><b>Пример:</b></p>
     * <pre>{@code
     * Image img = rm.loadResource(Image.class, Resources.DataTypes.IMAGE, "icon.main", new String[]{"64", "64", "true", "true"});
     * Font font = rm.loadResource(Font.class, Resources.DataTypes.FONT, "font.roboto", new String[]{"14"});
     * Parent ui = rm.loadResource(Parent.class, Resources.DataTypes.FXML, "main.window", new String[]{});
     * }</pre>
     *
     * @param <R> тип возвращаемого ресурса
     * @param resourceClazz класс ресурса (Image.class, Font.class и пр.)
     * @param resourceType строковый тип ресурса (IMAGE, FONT, FXML, SVG, FILE и др.)
     * @param resourceId идентификатор ресурса, ключ из properties
     * @param extensions дополнительные параметры (опционально)
     * @return загруженный объект ресурса или {@code null}
     */
    public <R> R loadResource(Class<R> resourceClazz, String resourceType, String resourceId, String[] extensions) {
        String resourcePath = resourcesPaths.get(resourceId);

        if (resourcePath == null || resourcePath.isEmpty()) {
            return null;
        }

        String lowerResourcePath = resourcePath.toLowerCase();

        boolean matchesType;

        switch (resourceType.toLowerCase()) {
            case Resources.Types.IMAGE:
                matchesType = lowerResourcePath.endsWith(".png") || lowerResourcePath.endsWith(".jpg") ||
                        lowerResourcePath.endsWith(".jpeg") || lowerResourcePath.endsWith(".gif") ||
                        lowerResourcePath.endsWith(".ico");
                break;
            case Resources.Types.STYLESHEET:
                matchesType = lowerResourcePath.endsWith(".css");
                break;
            case Resources.Types.FXML:
                matchesType = lowerResourcePath.endsWith(".fxml");
                break;
            case Resources.Types.FONT:
                matchesType = lowerResourcePath.endsWith(".ttf") || lowerResourcePath.endsWith(".otf");
                break;
            case Resources.Types.SVG:
                matchesType = lowerResourcePath.endsWith(".svg");
                break;
            case Resources.Types.FILE:
                matchesType = true;
                break;
            default:
                return null;
        }

        if (!matchesType) {
            return null;
        }

        File file = new File(getFullyPath(resourcePath));
        if (!file.exists()) {
            return null;
        }

        try {
            Object result;

            if (resourceClazz == Image.class) {
                int height = (extensions.length > 0) ? Integer.parseInt(extensions[0]) : 100;
                int width = (extensions.length > 1) ? Integer.parseInt(extensions[1]) : 100;
                boolean preserveRatio = extensions.length > 2 && Boolean.parseBoolean(extensions[2]);
                boolean smooth = extensions.length > 3 && Boolean.parseBoolean(extensions[3]);

                result = new Image(URLEncode(file.getAbsolutePath()), width, height, preserveRatio, smooth);
            } else if (resourceClazz == String.class && resourceType.equalsIgnoreCase(Resources.Types.STYLESHEET)) {
                result = URLEncode(file.getAbsolutePath());
            } else if (resourceClazz == Parent.class && resourceType.equalsIgnoreCase(Resources.Types.FXML)) {
                result = FXMLLoader.load(file.toURI().toURL());
            } else if (resourceClazz == Font.class && resourceType.equalsIgnoreCase(Resources.Types.FONT)) {
                int size = (extensions.length > 0) ? Integer.parseInt(extensions[0]) : 12;

                result = Font.loadFont(file.toURI().toString(), size);
            } else if (resourceClazz == String.class && resourceType.equalsIgnoreCase(Resources.Types.SVG)) {
                result = file.getAbsolutePath();
            } else if (resourceClazz == File.class && resourceType.equalsIgnoreCase(Resources.Types.FILE)) {
                result = file;
            } else {
                return null;
            }

            return resourceClazz.cast(result);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Удобная перегрузка {@link #loadResource(Class, String, String, String[])} без параметров extensions.
     * <p><b>Версия:</b> 0.1.4.4</p>
     *
     * <p><b>Пример использования:</b></p>
     * <pre>{@code
     * String css = rm.loadResource(String.class, Resources.DataTypes.STYLESHEET, "style.main");
     * }</pre>
     *
     * @param <R> тип возвращаемого ресурса
     * @param resourceClazz класс ресурса
     * @param resourceType тип ресурса
     * @param resourceId идентификатор ресурса
     * @return объект ресурса или null
     */
    public <R> R loadResource(Class<R> resourceClazz, String resourceType, String resourceId) {
        return loadResource(resourceClazz, resourceType, resourceId, new String[] {});
    }

    /**
     * Загружает ресурс, автоматически определяя его тип по классу, без явной необходимости указывать строковый тип.
     * <p>
     * Этот перегруженный метод использует класс {@code resourceClazz}, чтобы определить правильный тип ресурса
     * и вызвать основной метод {@link #loadResource(Class, String, String, String[])}.
     * </p>
     * <p>
     * Реализует интеллектуальное определение типа, уменьшая вероятность ошибок и облегчающи работу с API.
     * </p>
     * <p><b>Версия:</b> 0.1.4.4</p>
     *
     * <p><b>Пример использования:</b></p>
     * <pre>{@code
     * // Загрузка изображения по идентификатору
     * Image img = rm.loadResource(Image.class, "icon.main");
     *
     * // Загрузка шрифта
     * Font font = rm.loadResource(Font.class, "font.roboto");
     *
     * // Загрузка FXML
     * Parent node = rm.loadResource(Parent.class, "main.window");
     * }</pre>
     *
     * @param <R> тип ресурса, которого ожидаем (Image, Font, Parent, String, File)
     * @param resourceClazz класс ресурса, определяет внутренний тип (например, Image.class)
     * @param resourceId ключ ресурса, соответствующий имени в properties
     * @return загрузившийся ресурс или null при ошибке или несовпадении
     * @see #loadResource(Class, String, String, String[])
     * @since 0.1.4.4
     */
    public <R> R loadResource(Class<R> resourceClazz, String resourceId) {
        String resourceType = determineResourceType(resourceClazz);
        return loadResource(resourceClazz, resourceType, resourceId);
    }

    /**
     * Загрузить ресурс по идентификатору с автоматическим определением типа по расширению файла.
     * <p>
     * Метод анализирует расширение файла из карты ресурсов {@link #resourcesPaths} и
     * в зависимости от расширения приводит результат к подходящему типу.
     * Если расширение не распознано, возвращается ресурс как {@code Object}.
     * </p>
     * <p><b>Версия:</b> 0.1.4.4</p>
     *
     * <p><b>Работа метода:</b></p>
     * <ol>
     *   <li>Получает путь по ключу из {@link #resourcesPaths}.</li>
     *   <li>Определяет расширение файла из пути.</li>
     *   <li>Сопоставляет расширение с типом ресурса.</li>
     *   <li>Вызывает универсальный метод {@link #loadResource(Class, String, String, String[])} с типом.</li>
     *   <li>Если расширение неизвестно, вызывает тот же метод с возвращаемым типом {@code Object} (через Class&lt;Object&gt;).</li>
     * </ol>
     *
     * <p><b>Пример:</b></p>
     * <pre>{@code
     * Image img = rm.loadResource("icon.main");     // при .png/.jpg возвращает Image
     * Font font = rm.loadResource("font.roboto");  // при .ttf/.otf возвращает Font
     * Object unknown = rm.loadResource("file.abc"); // для неизвестного расширения возвращает Object
     * }</pre>
     *
     * @param <R> ожидаемый тип ресурса, будет сопоставлен исходя из расширения файла или Object
     * @param resourceId идентификатор ресурса в resourcesPaths
     * @return загруженный ресурс типизированный по расширению или Object, если тип неизвестен
     */
    @SuppressWarnings("unchecked")
    public <R> R loadResource(String resourceId) {
        String path = resourcesPaths.get(resourceId);
        if (path == null || path.isEmpty()) {
            return null;
        }

        String lowerPath = path.toLowerCase();
        int dotIndex = lowerPath.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == lowerPath.length() - 1) {
            return (R) loadResource(File.class, Resources.Types.FILE, resourceId);
        }

        String ext = lowerPath.substring(dotIndex + 1);

        try {
            switch (ext) {
                case "png":
                case "jpg":
                case "jpeg":
                case "gif":
                case "ico":
                    return (R) loadResource(Image.class, Resources.Types.IMAGE, resourceId);
                case "css":
                    return (R) loadResource(String.class, Resources.Types.STYLESHEET, resourceId);
                case "fxml":
                    return (R) loadResource(Parent.class, Resources.Types.FXML, resourceId);
                case "ttf":
                case "otf":
                    return (R) loadResource(Font.class, Resources.Types.FONT, resourceId);
                case "svg":
                    return (R) loadResource(String.class, Resources.Types.SVG, resourceId);
                default:
                    return (R) loadResource(File.class, Resources.Types.FILE, resourceId);
            }
        } catch (ClassCastException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Внутренний метод для определения типа ресурса по классу.
     *
     * @param clazz класс ресурса
     * @return строковое обозначение типа ресурса, исходя из переданного класса
     * @throws IllegalArgumentException если класс не поддерживается
     */
    private String determineResourceType(Class<?> clazz) {
        if (clazz == Image.class) {
            return Resources.Types.IMAGE;
        } else if (clazz == String.class) {
            // возможно, ресурс CSS или SVG, требует уточнения, но по умолчанию стиль
            return Resources.Types.STYLESHEET;
        } else if (clazz == Parent.class) {
            return Resources.Types.FXML;
        } else if (clazz == Font.class) {
            return Resources.Types.FONT;
        } else if (clazz == File.class) {
            return Resources.Types.FILE;
        } else {
            throw new IllegalArgumentException("Unsupported resource class: " + clazz);
        }
    }
    /**
     * Загружает CSS-ресурс как URL для подключения к JavaFX сцене.
     * <p><b>Версия:</b> 0.1.4.4</p>
     *
     * <p><b>Пример:</b></p>
     * <pre>{@code
     * String cssUrl = rm.loadStylesheet("style.main");
     * scene.getStylesheets().add(cssUrl);
     * }</pre>
     *
     * @param name имя CSS ресурса из properties
     * @return строка URL CSS или {@code null}
     */
    public String loadStylesheet(String name) {
        return loadResource(String.class, Resources.Types.STYLESHEET, name, new String[0]);
    }
    /**
     * Загружает SVG-файл, используя ConfigurationManager для поиска параметров.
     * <p>Возвращает первый найденный параметр, связанный с SVG.</p>
     * <p><b>Версия:</b> 0.1.4.4</p>
     *
     * <p><b>Пример:</b></p>
     * <pre>{@code
     * String svgPath = rm.loadSVG("icon.svg");
     * }</pre>
     *
     * @param id идентификатор SVG ресурса
     * @return строка с путем SVG или пустая строка
     */
    public String loadSVG(String id) {
        return FileManager.instance.findFirstParam(
                Path.of(loadResource(String.class, Resources.Types.SVG, id, new String[0])),
        "");
    }
    /**
     * Возвращает файл ресурса по идентификатору.
     * <p><b>Версия:</b> 0.1.4.4</p>
     *
     * <p><b>Пример:</b></p>
     * <pre>{@code
     * File configFile = rm.loadResourcePathFile("config.json");
     * if (configFile != null) {
     *   // Работа с файлом
     * }
     * }</pre>
     *
     * @param name идентификатор ресурса
     * @return объект File или {@code null}
     */
    public File loadResourcePathFile(String name) {
        String path = resourcesPaths.get(name);

        if (path == null)
            return null;

        return new File(getFullyPath(path));
    }
    /**
     * Загрузка изображения c параметрами размера, сохранения пропорций и сглаживания.
     * <p><b>Версия:</b> 0.1.4.4</p>
     *
     * <p><b>Пример:</b></p>
     * <pre>{@code
     * Image img = rm.loadImage("icon.main", 128, 128, true, true);
     * }</pre>
     *
     * @param image идентификатор изображения
     * @param height высота (пиксели)
     * @param width ширина (пиксели)
     * @param isPreserveRatio сохранить пропорции
     * @param isSmooth сглаживание
     * @return объект Image или null
     */
    public Image loadImage(String image, int height, int width, boolean isPreserveRatio, boolean isSmooth) {
        String[] extensions = new String[]{
                String.valueOf(height),
                String.valueOf(width),
                String.valueOf(isPreserveRatio),
                String.valueOf(isSmooth)
        };

        return loadResource(Image.class, Resources.Types.IMAGE, image, extensions);
    }
    /**
     * Загрузка шрифта с необходимым размером.
     * <p><b>Версия:</b> 0.1.4.4</p>
     *
     * <p><b>Пример:</b></p>
     * <pre>{@code
     * Font font = rm.loadFont("font.roboto", 16);
     * }</pre>
     *
     * @param font идентификатор шрифта
     * @param size размер шрифта, pt
     * @return Font или null
     */
    public Font loadFont(String font, int size) {
        return loadResource(Font.class, Resources.Types.FONT, font, new String[]{
                String.valueOf(size)
        });
    }
    /**
     * Загружает FXML-конструктор интерфейса, возвращая корневой элемент.
     * <p>Бросает IOException при ошибках доступа.</p>
     * <p><b>Версия:</b> 0.1.4.4</p>
     *
     * <p><b>Пример:</b></p>
     * <pre>{@code
     * Parent root = rm.loadFXML("main.window");
     * }</pre>
     *
     * @param path идентификатор FXML ресурса
     * @return Parent элемент
     * @throws IOException ошибка загрузки FXML
     */
    public Parent loadFXML(String path) throws IOException {
        return loadResource(Parent.class, Resources.Types.FXML, path, new String[0]);
    }
    /**
     * Получить {@link FXMLLoader} для дальнейшей загрузки и конфигурации.
     * <p><b>Версия:</b> 0.1.4.4</p>
     *
     * <p><b>Пример:</b></p>
     * <pre>{@code
     * FXMLLoader loader = rm.loadFxmlLoader("main.window");
     * Parent root = loader.load();
     * }</pre>
     *
     * @param path идентификатор FXML ресурса
     * @return FXMLLoader
     * @throws IOException ошибка доступа к файлу
     */
    public FXMLLoader loadFxmlLoader(String path) throws IOException {
        return new FXMLLoader(new File(getFullyPath(path)).toURI().toURL());
    }
    /**
     * Возвращает корректный URI для JavaFX из абсолтного пути.
     * <p><b>Версия:</b> 0.1.4.4</p>
     *
     * @param path абсолютный путь к файлу
     * @return URI в виде строки
     */
    private String URLEncode(String path) {
        return Paths.get(path).toUri().toString();
    }
    /**
     * Приводит путь к абсолютному виду.
     * <p><b>Версия:</b> 0.1.4.4</p>
     *
     * @param res путь
     * @return абсолютный путь
     */
    public String getFullyPath(String res) {
        return new File(res).getAbsolutePath();
    }
    /**
     * Конструктор класса, инициализирующий загрузку ресурсов из
     * файла <code>res/resources.properties</code>.
     * <p><b>Версия:</b> 0.1.4.4</p>
     */
    public ResourceManager(String path) {
        loadResources(path);
    }

    /**
     * Конструктор класса, инициализирующий загрузку ресурсов из
     * файла <code>res/resources.properties</code>.
     * <p><b>Версия:</b> 0.1.4.4</p>
     */
    public ResourceManager(Path res) {
        loadResources(res.toAbsolutePath().toString());
    }

    @Override
    public String toString() {
        return resourcesPaths.toString();
    }
}