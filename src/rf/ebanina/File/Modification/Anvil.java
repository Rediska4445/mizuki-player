package rf.ebanina.File.Modification;

import api.AudioMod;
import rf.ebanina.ebanina.Music;
import rf.ebanina.utils.loggining.logging;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * <h1>Anvil</h1>
 * Синглтон-класс для динамической загрузки и активации <b>модулей-модификаторов</b> из JAR-файлов.
 * <p>
 * Решает задачу <b>горячей подгрузки</b> Java-модулей без перезапуска приложения.
 * Сканирует указанную папку на наличие <code>*.jar</code>, извлекает <code>*.class</code>,
 * проверяет реализацию {@link AudioMod} и автоматически вызывает {@link AudioMod#applyMod()}.
 * </p>
 * <p>
 * <b>Критические особенности:</b>
 * <ul>
 *   <li>Поддержка <b>reflection API</b> для динамической инстанциации.</li>
 *   <li>Изоляция classloader'ов <b>по JAR</b> — предотвращает конфликты зависимостей.</li>
 *   <li>Фильтрация: только <b>конкретные реализации</b> {@link AudioMod} (не абстрактные классы/интерфейсы).</li>
 * </ul>
 * </p>
 * <p>
 * Класс помечен {@link logging} с тегом <code>"Mod Loader"</code> для детального логирования процесса.
 * </p>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * // В главном классе приложения или инициализаторе
 * Anvil.anvil.loadAllModsFromFolder("plugins/mods/");
 * // Автоматически загрузит и применит все AudioMod из JAR в папке
 * }</pre>
 *
 * <h3>Структура модуля-модификатора</h3>
 * <pre>{@code
 * public class MyAudioMod implements AudioMod {
 *     @Override
 *     public void applyMod() {
 *         // Модификация приложения: добавление UI, патчи, хуки и т.д.
 *     }
 * }
 * // Компилируется в MyAudioMod.jar и кладётся в папку mods/
 * }</pre>
 *
 * <h3>Обработка ошибок</h3>
 * <ul>
 *   <li>Невалидная папка: <code>return</code> без исключений.</li>
 *   <li>Ошибки JAR/ClassLoader/Reflection: <code>e.printStackTrace()</code> (логируется через {@link logging}).</li>
 *   <li>Не-AudioMod классы игнорируются молча.</li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 0.1.4.4
 * @see AudioMod
 * @see URLClassLoader
 * @see Class#forName(String, boolean, ClassLoader)
 */
@logging(tag = "Mod Loader")
public class Anvil
{
    /**
     * Глобальный синглтон-инстанс загрузчика модулей.
     * <p>
     * Позволяет вызывать {@link #loadAllModsFromFolder(String, String)} из любого места приложения
     * без создания новых экземпляров: <code>Anvil.anvil.loadAllModsFromFolder(path)</code>.
     * </p>
     * <p>
     * Инициализируется <b>лениво</b> при первом обращении (Eager Initialization).
     * Thread-safe по JVM спецификации для статических полей.
     * </p>
     *
     * @see AudioMod
     */
    public static Anvil anvil = new Anvil();
    /**
     * Загружает и активирует <b>все модули</b> из указанной папки.
     * <p>
     * Полный цикл обработки:
     * <ol>
     *   <li>Валидация папки: <code>exists() &amp;&amp; isDirectory()</code>.</li>
     *   <li>Фильтрация <code>*.jar</code> файлов.</li>
     *   <li><b>Для каждого JAR:</b>
     *     <ul>
     *       <li>Создание изолированного {@link URLClassLoader}.</li>
     *       <li>Сканирование всех <code>*.class</code> через {@link JarFile#entries()}.</li>
     *       <li>Reflection: {@link Class#forName(String, boolean, ClassLoader)}.</li>
     *       <li>Проверка: <code>AudioMod.isAssignableFrom() &amp;&amp; !abstract &amp;&amp; !interface</code>.</li>
     *       <li>Инстанциация: <code>getDeclaredConstructor().newInstance()</code>.</li>
     *       <li>Активация: <code>modInstance.applyMod()</code>.</li>
     *     </ul>
     *   </li>
     * </ol>
     * </p>
     * <p>
     * <b>Автоматическое закрытие ресурсов:</b> {@link JarFile} и {@link URLClassLoader} в try-with-resources.
     * </p>
     * <p>
     * Исключения обрабатываются локально: <code>printStackTrace()</code> для продолжения загрузки других JAR.
     * </p>
     *
     * @param modsFolderPath путь к папке с JAR-модулями (например, <code>"mods/"</code>)
     */
    public void loadAllModsFromFolder(String modsFolderPath, String ignoredModsCsv) {
        File modsDir = new File(modsFolderPath);

        if (!modsDir.exists() || !modsDir.isDirectory()) {
            return;
        }

        Music.mainLogger.info("Ignored list: " + ignoredModsCsv);

        Set<String> ignoredMods = new HashSet<>();
        if (ignoredModsCsv != null && !ignoredModsCsv.trim().isEmpty()) {
            Arrays.stream(ignoredModsCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(ignoredMods::add);
        }

        Music.mainLogger.info(ignoredMods);

        File[] files = modsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (files == null) {
            return;
        }

        for (File jarFile : files) {
            try {
                URL[] urls = {
                        jarFile.toURI().toURL()
                };

                String modFileName = jarFile.getName().toLowerCase();
                String modNameFromFile = modFileName.substring(0, modFileName.lastIndexOf(".jar"));

                Music.mainLogger.info(modNameFromFile);

                if (ignoredMods.contains(modNameFromFile)) {
                    Music.mainLogger.println("[Mods] Skipped (ignored): " + jarFile.getName());
                    continue;
                }

                try (URLClassLoader loader = new URLClassLoader(urls, Anvil.class.getClassLoader());
                    JarFile jar = new JarFile(jarFile)) {

                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();

                        if (name.equals("module-info.class")) {
                            continue;
                        }

                        if (name.endsWith(".class")) {
                            String className = name.replace("/", ".").replace(".class", "");

                            try {
                                Class<?> clazz = Class.forName(className, false, loader);

                                if (AudioMod.class.isAssignableFrom(clazz) && !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                                    AudioMod modInstance = (AudioMod) clazz.getDeclaredConstructor().newInstance();

                                    modInstance.applyMod();
                                }
                            } catch (Throwable e) {
                                Music.mainLogger.println("[Mods] Не удалось просканировать класс: " + className);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
