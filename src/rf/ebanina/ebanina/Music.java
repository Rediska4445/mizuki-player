package rf.ebanina.ebanina;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.sun.jna.platform.win32.Kernel32;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Modification.Anvil;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Animations;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.KeyBindings.KeyBindingController;
import rf.ebanina.ebanina.KeyBindings.Keys;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Media;
import rf.ebanina.utils.loggining.Log;
import rf.ebanina.utils.loggining.logging;
import rf.ebanina.utils.weakly.WeakConst;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static rf.ebanina.UI.Root.scene;

/**
 * <h1>Music</h1>
 * Главный класс приложения Ebanina — полнофункциональный музыкальный плеер с поддержкой плейлистов,
 * горячих клавиш, модов и расширенной кастомизацией.
 * <p>
 * {@code Music} расширяет {@link Application} и реализует полный жизненный цикл GUI-приложения:
 * от инициализации ресурсов и восстановления сессии до обработки закрытия с сохранением состояния.
 * Название "Ebanina" появилось в середине 2024 года при попытке упаковки проекта.
 * </p>
 * <p>
 * <b>Ключевые возможности:</b>
 * <ul>
 *   <li>Глобальные горячие клавиши через JNativeHook ({@link GlobalScreen}).</li>
 *   <li>Система плейлистов с историей воспроизведения и кэшированием.</li>
 *   <li>Автоматическое восстановление позиции окна, плейлиста и трека.</li>
 *   <li>Поддержка модов через {@link Anvil}.</li>
 *   <li>Динамическая смена иконки/заголовка по текущему треку.</li>
 *   <li>Расширенная анимационная система ({@link Animations}).</li>
 * </ul>
 * </p>
 * <p>
 * Класс {@code final} — не предназначен для наследования. Все зависимости статические синглтоны.
 * Метод {@link #start(Stage)} содержит полную последовательность инициализации (~500-800 мс).
 * </p>
 *
 * <h2>Жизненный цикл</h2>
 * <pre>{@code
 * Start.main() → Application.launch() → Music.start() → Root.rootImpl.init() → UI готово
 * Закрытие: stage.setOnCloseRequest({@link #exitEvent}) → save → System.exit()
 * }</pre>
 *
 * <h2>Состояние при запуске</h2>
 * <ul>
 *   <li>Восстанавливается позиция окна, последний плейлист/трек.</li>
 *   <li>Загружается кэш медиа, история воспроизведения.</li>
 *   <li>Регистрируются глобальные хуки клавиш/мыши.</li>
 *   <li>Инициализируется медиапроцессор с текущим треком.</li>
 * </ul>
 *
 * <h2>Особенности архитектуры</h2>
 * <ul>
 *   <li><b>Синглтон-ориентировано</b> — {@link FileManager#instance}, {@link PlayProcessor#playProcessor}.</li>
 *   <li><b>Статические ссылки</b> — {@link Root#stage}, {@link Root#scene} (TODO: рефакторинг).</li>
 *   <li><b>Глобальная обработка ошибок</b> — перехват FX-ошибок с аварийным закрытием.</li>
 *   <li><b>Персистентность</b> — состояние сохраняется при каждом закрытии.</li>
 * </ul>
 *
 * @author Ebanina Std.
 * @version v1.4.7 (Mizuki)
 * @since 1.0.0
 * @see Application
 * @see KeyBindingController
 * @see MediaProcessor
 * @see PlayProcessor
 * @see FileManager
 * @see Root
 */
@logging(tag = "Main Loader Class", fileOut = true)
public final class Music
        extends Application
{
    /**
     * Версия приложения в формате <strong>SemVer</strong> (Semantic Versioning): {@code MAJOR.MINOR.PATCH}.
     *
     * <h3>Правила версионирования</h3>
     *
     * <ul>
     *   <li><strong>MAJOR</strong> ↑ — несовместимые изменения API<br>
     *       <em>Пример: смена формата данных, удаление методов</em></li>
     *
     *   <li><strong>MINOR</strong> ↑ — новый функционал (совместимо)<br>
     *       <em>Пример: добавление новых методов, улучшение UI</em></li>
     *
     *   <li><strong>PATCH</strong> ↑ — исправления багов<br>
     *       <em>Пример: фиксы ошибок, мелкие улучшения</em></li>
     * </ul>
     *
     * <p><strong>Пример:</strong> {@code 2.3.1} = MAJOR 2 + MINOR 3 + 1 багфикс</p>
     *
     * <p><em><strong>Название приложения меняется с MINOR версиями</strong></em></p>
     *
     * @since 1.0.0
     */
    public static final String version = "v1.4.7";
    // next - -Momoka-, or -Noewa-, or -Teveo-, or Tsunuma, or Dozuki, or Naomi, or Tsurukawa, or Casumi, or Tsunawi, or Tovokado, or -Natachi-, or Nanami

    /**
     * Название версии.
     *
     * <p>
     *     Название версии ничего не значит, и может быть выведено.
     * </p>
     */
    public static final String versionName = "Mizuki";

    /**
     * Официальное название приложения.
     *
     * <p>
     *     Используется в заголовке окна.
     *
     * Название {@literal Ebanina}, пришло когда я в +- середине 2024-го пытался упаковать проект в запускаемое приложение.
     * <p>
     * После этого, я потратил около 5-6 часов, на то, чтобы просто вернуться туда, откуда начал
     */
    public static final String appName = "Ebanina (Mizuki)";

    /**
     * Название приложение, которое будет отображаться в заголовке окна.
     *
     * <p>
     *     Внутри происходит следующее:
     *     <p>
     *     - Название - {@link Music#appName};
     *     <p>
     *     - Версию - {@link Music#version};
     *     <p>
     *     - Название версии - {@link Music#versionName};
     * </p>
     *
     */
    public static final String name = appName + ": " + version + " - " + versionName;

    /**
     * Событие вызывающийся при закрытии приложения.
     *
     * <p>
     *     Внутри происходит следующее:
     *     <p>
     *     - Закрытие плеера {@link MediaProcessor#mediaPlayer};
     *     <p>
     *     - Остановка слайдера {@link Root#soundSlider};
     *     <p>
     *     - Очистка кеша {@link FileManager#clearCacheData(String)};
     *     <p>
     *     - Сохранение общих данных {@link FileManager#saveSharedData()};
     *     <p>
     *     - Сохранение истории в файл {@link rf.ebanina.ebanina.Player.TrackHistory#saveToFile(File)};
     *     <p>
     *     - Выход из приложения (закрытие процесса) {@link Kernel32#ExitProcess(int)};
     * </p>
     *
     * <p>
     * <h3>
     *     Эта переменная должна быть изменена или удалена.
     * </h3>
     * </p>
     */
    public static javafx.event.EventHandler<javafx.stage.WindowEvent> exitEvent = windowEvent -> {
        // Закрытие плеера
        for (PluginWrapper p : MediaProcessor.mediaProcessor.mediaPlayer.getPlugins()) {
            p.destroy();
        }

        if (MediaProcessor.mediaProcessor.mediaPlayer != null)
            MediaProcessor.mediaProcessor.mediaPlayer.close();

        // Закрытие графического слайдера плеера
        Root.SliderHandler.stop();

        // Очистка кеша из inet папки
        FileManager.instance.clearCacheData(Resources.Properties.DEFAULT_INET_CACHE_PATH.getKey());

        // Сохранение общих данных о приложении
        FileManager.instance.saveSharedData();

        // Сохранение истории в файл
        if(PlayProcessor.playProcessor.getTrackHistoryGlobal() != null) {
            PlayProcessor.playProcessor.getTrackHistoryGlobal().saveToFile(new File(Resources.Properties.HISTORY_FILE_PATH.getKey()));
        }

        //TODO: Выяснить, почему оно вызывает лаг
        //      System.exit(130);
        Kernel32.INSTANCE.ExitProcess(130);
    };

    /**
     * Константа, которая фиксирует точку времени для определения длительности использования.
     * <p>
     * Слабая константа ({@link WeakConst}) так как она не требует конструктор.
     *
     * <p>
     * <h3>Эта переменная не должна изменяться.</h3>
     *
     * <p>
     * Для времени используется UNIX-время, которое вызывается через {@link System#currentTimeMillis()}.
     */
    public static WeakConst<Long> startTimeMs = new WeakConst<>();

    /**
     * Основной логгер приложения Ebanina.
     *
     * <p>Используется для структурированного логирования жизненного цикла приложения,
     * инициализации компонентов, ошибок и профилирования производительности.</p>
     *
     * <h3>Уровни логирования:</h3>
     * <ul>
     *   <li>{@code mainLogger.info()} — ключевые этапы инициализации</li>
     *   <li>{@code mainLogger.warn()} — некритичные проблемы</li>
     *   <li>{@code mainLogger.severe()} — критические ошибки</li>
     *   <li>{@code mainLogger.profiler()} — замеры производительности</li>
     *   <li>{@code mainLogger.printf()} — динамические события</li>
     * </ul>
     *
     * <p><b>Пример использования:</b></p>
     * <pre>{@code
     * mainLogger.info("Application startup: %s", version);
     * mainLogger.profiler("UI init: %.0f ms", durationMs);
     * mainLogger.severe("Global hook failed: %s", ex.getMessage());
     * }</pre>
     *
     * @see rf.ebanina.utils.loggining.ILogging
     * @see Log
     */
    public static Log mainLogger = new Log();

    /**
     * Основной метод запуска GUI-приложения.
     *
     * <p>
     * Этот метод определён в {@linkplain Application#start(Stage)}.
     * Он является основным для инициализации всего приложения.
     *
     * <p>
     * <h3>Этот метод {@link Application#start(Stage)}, также инициализирует всё приложение.</h3>
     * Этот метод вызывается из {@link rf.ebanina.Start}, как единственный.
     * Отдельный класс для запуска этого метода необходим по требованию JavaFX.
     *
     * @param stage нужен для JavaFX
     * @throws IOException при различных ошибках внутри метода
     */
    @Override
    public void start(Stage stage) throws IOException {
        mainLogger.info("Application v%s (%s) STARTUP\n", version, versionName);

        FileManager.createCacheDirectoryIfNotExist();

        // Установка времени при старте.
        // Это будет определять временную точку для дельты времени.
        startTimeMs.set(System.currentTimeMillis());

        mainLogger.printf("Application start timestamp: %d ms\n", startTimeMs.get());

        // Инициализация файлового вывода логов.
        // Без вызова этого метода, логи не будут выводится в файл.
        mainLogger.init_file_log();

        mainLogger.info("File logging initialized");

        // Перехват всех исключений из потоков.
        // Это сделано для отслеживания NPE из JavaFX.
        // Это вряд-ли поможет, но хоть может.
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            mainLogger.severe("Uncaught exception in thread '%s': %s", thread.getName(), e.getMessage());
            e.printStackTrace();

            if (e instanceof IllegalStateException ise) {
                mainLogger.severe("FX THREAD VIOLATION on: %s", thread.getName());

                stage.close();
                System.exit(1);
            }
        });

        // Ссылка на объект приложения.
        // Используется во всём приложении, не должно меняться по идеи.
        Root.stage = stage;

        mainLogger.info("Root stage reference set");

        // Ссылка на сцену приложения.
        // Не должно меняться, используется во всем приложении, хотя не должно.
        scene = new Scene(Root.rootImpl.getRoot());

        mainLogger.info("Main scene created with root node");

        // Параметры приложение
        stage.setTitle(name);
        stage.setScene(scene);

        mainLogger.info("Stage configured with title: %s", name);

        // Последние данные о положении и размерах
        try {
            stage.setX(Float.parseFloat(FileManager.instance.readSharedData().get("layout_x")));
            stage.setY(Float.parseFloat(FileManager.instance.readSharedData().get("layout_y")));
            stage.setWidth(Float.parseFloat(FileManager.instance.readSharedData().get("width")));
            stage.setHeight(Float.parseFloat(FileManager.instance.readSharedData().get("height")));
        } catch (Exception e) {
            mainLogger.warn("Failed to restore window geometry: %s", e.getMessage());
        }

        mainLogger.info("Window geometry restored from session");

        // Это будет так!
        // Мне реально похуй!
        stage.setMaxWidth((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()*2/3));
        stage.setMaxHeight((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()*2/3*9/16));
        stage.show();

        mainLogger.info("Stage displayed with max constraints");

        // Инициализировать контроллер анимаций.
        // В частности, эта хуйня оптимизирует работу анимаций (см. класс)
        Animations.init(stage);

        mainLogger.info("Animations controller initialized");

        // При закрытии закрыть все потоки и записать кеш.
        stage.setOnCloseRequest(exitEvent);

        mainLogger.info("Exit event handler registered");

        // Иконка меняется, в соответствии с текущей обложкой трека.
        // Прикольная идея возникла ещё в 2022 году.
        stage.focusedProperty().addListener((ov, onHidden, onShown) -> {
            try {
                stage.getIcons().clear();

                if (onHidden /* Если хуйня скрыта */) {
                    stage.setTitle(Root.currentArtist.getText() + " - " + Root.currentTrackName.getText());
                    stage.getIcons().setAll(Root.art.getImage());

                    mainLogger.printf("Focus lost - Track title: %s", stage.getTitle());
                } else {
                    stage.setTitle(name);
                    stage.getIcons().setAll(ColorProcessor.logo);

                    mainLogger.info("Focus gained - App title restored");
                }
            } catch (Exception e) {
                // Необязательно, но в случае исключения - выглядит некрасиво
                mainLogger.warn("Focus listener error: %s", e.getMessage());

                stage.setTitle(name);
                stage.getIcons().setAll(ColorProcessor.logo);
            }
        });

        // Эта хуйня пишет файл для графического отображения настроек.
        // На основе того что есть в файле настроек, он будет создавать окно настроек в приложении.
        // P.S. Я не предполагаю что приложение можно будет так просто настраивать (это не интересно).
        //      Файл содержит множество строк настроек, большинство из них просто не написаны,
        //      и я не помню, что я там добавлял.
        //      Мне похуй, я знаю как приложение настроить, через файл, а окно добавил просто так (оно вполне может не работать).
        ConfigurationManager.fxmlConverter.convert();

        mainLogger.info("Configuration FXML converter executed");

        //<------------------------Ручная подготовка индексатора-------------------------->//

        mainLogger.info("Playlist cursorus prepare");

        // Курсор на текущий плейлист
        PlayProcessor.playProcessor.setCurrentPlaylistIter(Integer.parseInt(FileManager.instance.readSharedData().get("last_track_index_playlist_local")));
        // Курсор на текущий трек
        PlayProcessor.playProcessor.setTrackIter(Integer.parseInt(FileManager.instance.readSharedData().get("last_track_index_local")));

        mainLogger.info("Playlist iter: %d, Track iter: %d",
                PlayProcessor.playProcessor.getCurrentPlaylistIter(),
                PlayProcessor.playProcessor.getTrackIter()
        );

        try {
            FileManager.instance.setPlaylists(PlayProcessor.playProcessor.getCurrentDefaultMusicDir());

            PlayProcessor.playProcessor.setCurrentMusicDir(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath());

            mainLogger.info("Playlist and tracks loaded from: %s", PlayProcessor.playProcessor.getCurrentMusicDir());

            PlayProcessor.playProcessor.getTracks().clear();
            PlayProcessor.playProcessor.getTracks().addAll(FileManager.instance.getMusic(Paths.get(PlayProcessor.playProcessor.getCurrentMusicDir())));
        } catch (IndexOutOfBoundsException e) {
            PlayProcessor.playProcessor.setCurrentMusicDir(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.setCurrentPlaylistIter(0).getCurrentPlaylistIter()).getPath());

            mainLogger.warn("Playlist index out of bounds, resetting to 0");
        } catch (Exception e) {
            e.printStackTrace();

            mainLogger.severe("Playlist restoration failed: %s", e.getMessage());
        }

        if (PlayProcessor.playProcessor.getCurrentPlaylistIter() >= PlayProcessor.playProcessor.getCurrentPlaylist().size()
                || PlayProcessor.playProcessor.getTrackIter() >= PlayProcessor.playProcessor.getTracks().size()
                || PlayProcessor.playProcessor.getCurrentPlaylistIter() < 0
                || PlayProcessor.playProcessor.getTrackIter() < 0) {
            PlayProcessor.playProcessor.setCurrentPlaylistIter(0);
            PlayProcessor.playProcessor.setTrackIter(0);
        }

        //<-------------------------------------------------------------------------------->//

        // Регистрация нативного кода, для отлова горячих клавиш
        try {
            GlobalScreen.registerNativeHook();

            mainLogger.info("GlobalScreen native hook registered successfully");
        } catch (NativeHookException ex) {
            Music.mainLogger.err(ex);

            mainLogger.severe("Failed to register GlobalScreen hook: %s", ex.getMessage());
        }

        // Хуйня
        KeyBindingController c = new KeyBindingController();

        mainLogger.info("KeyBindingController initialized and listeners registered");

        // Загрузка собственных горячих клавиш
        Keys.instance.loadBindings();

        // Инициализация слушателей горячих клавиш
        GlobalScreen.addNativeKeyListener(c);
        GlobalScreen.addNativeMouseListener(c);
        GlobalScreen.addNativeMouseWheelListener(c);

        // Инициализация графических компонентов
        mainLogger.profiler("Root UI initialization start");

        Root.rootImpl.init(); // <- ~290 ms

        mainLogger.profiler("Root UI initialization complete");

        // Загрузка кеша
        MediaProcessor.mediaProcessor.initGlobalMap();

        mainLogger.info("Media cache (global map) initialized");

        // Установка новой медиа
        MediaProcessor.mediaProcessor.setNewMedia(
                new Media(new File(PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()).getPath()).toURI().toString())
        );

        mainLogger.info("Current track media loaded");

        // Обновление медиаплеера
        MediaProcessor.mediaProcessor.updateMediaPlayer();

        mainLogger.info("MediaPlayer updated with new track");

        // Инициализация графики
        Root.rootImpl.set();

        mainLogger.info("Root UI final setup complete");

        // Нахуй не нужные классы.
        // Их нужно удалить, методы разнести по классам
        Root.PlaylistHandler.initialize();
        Root.ButtonHandler.initialize();
        Root.SliderHandler.initialize();

        mainLogger.info("All UI handlers initialized");

        // Инициализация медиа-контроллера (не самого, а того что логически к нему подходит)
        MediaProcessor.mediaProcessor.initialize();

        mainLogger.info("MediaProcessor fully initialized");

        // Нужно для чего то
        scene.setFill(Color.TRANSPARENT);

        mainLogger.info("Scene finalized and key listeners setup");

        // Горячие клавиши
        KeyBindingController.setupKeyListeners(stage.getScene());

        // Сессия
        FileManager.instance.loadSession();

        mainLogger.info("User session loaded");

        // Моды
        if(ConfigurationManager.instance.getBooleanItem("mod_load", "false")) {
            Anvil.anvil.loadAllModsFromFolder(ResourceManager.Instance.resourcesPaths.get(Resources.Properties.MODS.getKey()));
        }

        mainLogger.info("All mods loaded from folder");
        mainLogger.profiler("Application ENDUP Time");
    }
}