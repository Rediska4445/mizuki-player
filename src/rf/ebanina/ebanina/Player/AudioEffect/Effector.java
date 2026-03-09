package rf.ebanina.ebanina.Player.AudioEffect;

import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.AudioEffect.Plugins.Panix;
import rf.ebanina.ebanina.Player.AudioEffect.Plugins.VolumeNormalizer;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.utils.loggining.logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * <h1>Effector</h1>
 * Централизованный менеджер аудиоэффектов, реализованный как синглтон.
 * <p>
 * Этот класс является ядром системы аудиообработки в приложении, отвечающим за:
 * <ul>
 *   <li><b>Хранение</b> всех доступных аудиоэффектов (встроенных и загруженных).</li>
 *   <li><b>Загрузку</b> пользовательских плагинов из файловой системы.</li>
 *   <li><b>Сериализацию</b> и <b>десериализацию</b> эффектов для сохранения их состояния.</li>
 * </ul>
 * </p>
 * <p>
 * Класс следует паттерну <i>Singleton</i> — единственный экземпляр доступен через
 * статическое поле {@link #instance}. Это гарантирует, что все части приложения
 * работают с одной и той же коллекцией эффектов.
 * </p>
 * <p>
 * Все эффекты должны реализовывать интерфейс {@link IAudioEffect}. Класс не проверяет
 * тип объектов во время выполнения, кроме как при десериализации.
 * </p>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * // Получение экземпляра
 * Effector effector = Effector.instance;
 *
 * // Добавление нового эффекта
 * IAudioEffect reverb = new ReverbPlugin();
 * effector.plugins.add(reverb);
 *
 * // Сохранение эффекта в файл
 * effector.save(reverb, new File("plugins/reverb.plugin"));
 *
 * // Загрузка всех плагинов из директории
 * effector.pluginsLoad();
 * }</pre>
 *
 * <h3>Жизненный цикл</h3>
 * <p>
 * При создании экземпляра (в статическом инициализаторе) вызывается {@link #pluginsLoad()},
 * который сканирует директорию плагинов и загружает все файлы с расширением <code>.plugin</code>.
 * Встроенные эффекты (например, {@link VolumeNormalizer}, {@link Panix}) добавляются в список
 * {@link #plugins} при инициализации.
 * </p>
 *
 * <h3>Ограничения</h3>
 * <ul>
 *   <li>Класс не обеспечивает потокобезопасность — все операции должны выполняться из одного потока.</li>
 *   <li>Методы ввода-вывода не выбрасывают исключения, а только выводят стек в {@code System.err}.</li>
 *   <li>Нет механизма валидации или проверки целостности загруженных плагинов.</li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 0.1.4.2.5
 * @version 0.1.4.4
 * @see IAudioEffect
 * @see #instance
 * @see #plugins
 */
@logging(tag = "Effector", isActive = false, fileOut = false)
public class Effector {
    /**
     * Единственный экземпляр класса, предоставляющий глобальный доступ к менеджеру эффектов.
     * <p>
     * Создаётся при загрузке класса и инициализируется в приватном конструкторе.
     * Это гарантирует, что в приложении существует только один список эффектов.
     * </p>
     * <p>
     * Использование синглтона оправдано, так как аудиоэффекты — это глобальное состояние,
     * которое должно быть доступно из любого компонента плеера.
     * </p>
     *
     * @see #Effector()
     * @since 1.4.2.5
     */
    public static Effector instance = new Effector();
    /**
     * Список всех доступных аудиоэффектов, встроенных и загруженных.
     * <p>
     * Содержит экземпляры классов, реализующих {@link IAudioEffect}. Встроенные эффекты
     * добавляются при создании экземпляра {@link #instance}. Пользовательские плагины
     * загружаются из директории при вызове {@link #pluginsLoad()}.
     * </p>
     * <p>
     * Список является открытым и может быть модифицирован напрямую. Это позволяет
     * легко добавлять и удалять эффекты, но требует осторожности.
     * </p>
     *
     * @see #pluginsLoad()
     * @see #save(IAudioEffect, File)
     * @since 1.4.3
     */
    private Effector() {
        Music.mainLogger.info("Инициализация менеджера аудиоэффектов...");

        pluginsLoad();

        Music.mainLogger.info("Менеджер аудиоэффектов инициализирован. Загружено встроенных плагинов: " + plugins.size());
    }
    /**
     * Список всех доступных аудиоэффектов, встроенных и загруженных.
     * <p>
     * Содержит экземпляры классов, реализующих {@link IAudioEffect}. Встроенные эффекты
     * добавляются при создании экземпляра {@link #instance}. Пользовательские плагины
     * загружаются из директории при вызове {@link #pluginsLoad()}.
     * </p>
     * <p>
     * Список является открытым и может быть модифицирован напрямую. Это позволяет
     * легко добавлять и удалять эффекты, но требует осторожности.
     * </p>
     *
     * @see #pluginsLoad()
     * @see #save(IAudioEffect, File)
     * @since 1.4.2.5
     */
    public List<IAudioEffect> plugins = new ArrayList<>(List.of(
            new Panix(),
            new VolumeNormalizer()
    ));
    /**
     * Сохраняет состояние указанного аудиоэффекта в файл.
     * <p>
     * Использует стандартную сериализацию Java ({@link ObjectOutputStream}) для
     * преобразования объекта {@link IAudioEffect} в последовательность байтов и записи
     * в указанный файл.
     * </p>
     * <p>
     * <b>Важно</b>: эффект должен быть сериализуемым (реализовывать {@link Serializable}).
     * В противном случае будет выброшено {@link NotSerializableException}.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * IAudioEffect pitch = new Pitcher();
     * pitch.setSemitones(2.0f);
     * effector.save(pitch, new File("plugins/pitch_up.plugin"));
     * }</pre>
     *
     * @param plug эффект для сохранения; должен быть сериализуемым
     * @param file файл, в который будет сохранён эффект; не должен быть <code>null</code>
     * @throws NullPointerException если <code>file</code> равен <code>null</code>
     * @see #load(File)
     * @see ObjectOutputStream
     * @since 1.4.2.5
     */
    public void save(IAudioEffect plug, File file) {
        Music.mainLogger.println("Сохранение плагина '" + plug.getClass().getSimpleName() + "' в файл: " + file.getAbsolutePath());

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(plug);

            Music.mainLogger.println("Плагин успешно сохранён: " + file.getName());
        } catch (IOException e) {
            Music.mainLogger.severe("Ошибка при сохранении плагина в файл: " + file.getName() + " - " + e.getMessage());

            e.printStackTrace();
        }
    }
    /**
     * Загружает все пользовательские плагины из директории плагинов.
     * <p>
     * Основной метод инициализации. Выполняет следующие действия:
     * <ol>
     *   <li>Получает путь к директории плагинов из {@link ResourceManager}.</li>
     *   <li>Проверяет существование и тип директории.</li>
     *   <li>Сканирует директорию с помощью {@link Files#list(Path)}.</li>
     *   <li>Фильтрует файлы по расширению <code>.plugin</code>.</li>
     *   <li>Для каждого подходящего файла вызывает {@link #load(File)}.</li>
     *   <li>Добавляет успешно загруженный эффект в список {@link #plugins}.</li>
     * </ol>
     * </p>
     * <p>
     * Метод вызывается автоматически в приватном конструкторе, но может быть
     * вызван вручную для перезагрузки плагинов.
     * </p>
     *
     * <h3>Структура директории</h3>
     * <pre>{@code
     * resources/
     *   plugins/
     *     reverb.plugin
     *     chorus.plugin
     * }</pre>
     *
     * @see #load(File)
     * @see #plugins
     * @see ResourceManager
     * @since 1.4.3
     */
    public void pluginsLoad() {
        Path pluginsDir = Paths.get(ResourceManager.Instance.resourcesPaths.get("pluginsPath"));
        Music.mainLogger.println("Начало загрузки пользовательских плагинов из директории: " + pluginsDir);

        if (Files.exists(pluginsDir) && Files.isDirectory(pluginsDir)) {
            try (Stream<Path> files = Files.list(pluginsDir)) {
                long pluginCount = files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".plugin"))
                    .peek(path -> Music.mainLogger.println("Обнаружен плагин: " + path.getFileName()))
                    .map(path -> {
                        IAudioEffect plug = load(path.toFile());

                        if (plug != null) {
                            plugins.add(plug);
                            Music.mainLogger.println("Плагин загружен и добавлен: " + plug.getClass().getSimpleName());

                            return 1;
                        } else {
                            Music.mainLogger.warn("Не удалось загрузить плагин: " + path.getFileName());
                            return 0;
                        }
                    })
                    .reduce(0, Integer::sum);

                Music.mainLogger.info("Загрузка плагинов завершена. Успешно загружено: " + pluginCount + ", Всего в менеджере: " + plugins.size());
            } catch (IOException e) {
                Music.mainLogger.severe("Ошибка при сканировании директории плагинов: " + pluginsDir + " - " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Music.mainLogger.warn("Директория плагинов не существует или не является директорией: " + pluginsDir);
        }
    }
    /**
     * Загружает аудиоэффект из указанного файла.
     * <p>
     * Использует стандартную десериализацию Java ({@link ObjectInputStream}) для
     * восстановления объекта {@link IAudioEffect} из файла.
     * </p>
     * <p>
     * Перед загрузкой проверяет существование файла и то, что это обычный файл.
     * После десериализации проверяет тип объекта с помощью <code>instanceof</code>.
     * </p>
     * <p>
     * При возникновении ошибки (например, повреждённый файл) метод возвращает <code>null</code>
     * и подавляет исключение, выводя стек в {@code System.err}.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * IAudioEffect savedPitch = effector.load(new File("plugins/pitch_up.plugin"));
     * if (savedPitch != null) {
     *     effector.plugins.add(savedPitch);
     * }
     * }</pre>
     *
     * @param file файл, из которого будет загружен эффект; не должен быть <code>null</code>
     * @return загруженный эффект или <code>null</code>, если файл не существует, повреждён или содержит несовместимый объект
     * @throws NullPointerException если <code>file</code> равен <code>null</code>
     * @see #save(IAudioEffect, File)
     * @see ObjectInputStream
     * @since 1.4.2.5
     */
    public IAudioEffect load(File file) {
        if (!file.exists() || !file.isFile()) {
            Music.mainLogger.warn("Файл плагина не существует или не является файлом: " + file.getAbsolutePath());
            return null;
        }

        Music.mainLogger.println("Попытка загрузки плагина из файла: " + file.getName());

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();

            if (obj instanceof IAudioEffect) {
                Music.mainLogger.println("Файл успешно десериализован как IAudioEffect: " + file.getName());
                return (IAudioEffect) obj;
            } else {
                Music.mainLogger.warn("Загруженный объект не является IAudioEffect, тип: " + obj.getClass().getName());
            }
        } catch (IOException | ClassNotFoundException e) {
            Music.mainLogger.severe("Исключение при загрузке плагина из файла: " + file.getName() + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        return null;
    }
}
