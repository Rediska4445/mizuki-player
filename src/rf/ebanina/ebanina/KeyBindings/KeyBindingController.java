package rf.ebanina.ebanina.KeyBindings;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.UI.Editors.Network.NetworkHost;
import rf.ebanina.UI.Editors.Player.AudioHost;
import rf.ebanina.UI.Editors.Settings.Settings;
import rf.ebanina.UI.Editors.Statistics.Track.TrackStatistics;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.utils.loggining.logging;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static rf.ebanina.Network.APIS.GeniusAPI.Search.openGeniusLyrics;
import static rf.ebanina.ebanina.KeyBindings.KeyBindingProcessor.isKeyPressed;
import static rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor.playProcessor;

/**
 * <h1>KeyBindingController</h1>
 * Синглтон-менеджер всех горячих клавиш приложения с поддержкой динамической загрузки из файлов.
 * <p>
 * {@code KeyBindingController} содержит два списка комбинаций:
 * <ul>
 *   <li>{@link #hotKeys} — <b>глобальные</b> клавиши (работают поверх всех окон).</li>
 *   <li>{@link #sceneHotKeys} — <b>контекстные</b> клавиши (только при активном окне).</li>
 * </ul>
 * </p>
 * <p>
 * <b>Основная логика:</b>
 * <ul>
 *   <li>Загрузка дополнительных клавиш из директории {@link ConfigurationManager#hotKeysDirectoryPath}.</li>
 *   <li>Динамическая проверка комбинаций через {@link KeyBindingProcessor#pressed_keys}.</li>
 *   <li>Поддержка предикатов {@link HotKey#setPredicate(Supplier)}} для условной активации.</li>
 *   <li>Обработка через {@link #globalKeys(NativeKeyEvent)} и {@link #sceneKeys()}.</li>
 * </ul>
 * </p>
 * <p>
 * Все вызовы UI выполняются через {@link Platform#runLater(Runnable)} ()} для thread-safety.
 * Синглтон {@link #instance} регистрируется в {@link rf.ebanina.ebanina.Music#start(Stage)}.
 * </p>
 *
 * <h2>Глобальные клавиши (hotKeys)</h2>
 * <table>
 *   <tr><th>IID</th><th>Комбинация</th><th>Действие</th></tr>
 *   <tr><td>skip_audio_intro</td><td>Shift+Alt+M</td><td>Пропуск интро</td></tr>
 *   <tr><td>play_hotkey</td><td>NumLock, MediaPlay</td><td>Play/Pause</td></tr>
 *   <tr><td>tempo_change</td><td>Shift+Alt+PgUp/Dn</td><td>Темп ±5%</td></tr>
 *   <tr><td>next/prev</td><td>MediaNext/Prev</td><td>След/пред трек</td></tr>
 *   <tr><td>skip_pit</td><td>Shift+Alt+B</td><td>Пропуск пит</td></tr>
 * </table>
 *
 * <h2>Контекстные клавиши (sceneHotKeys)</h2>
 * <table>
 *   <tr><th>IID</th><th>Комбинация</th><th>Действие</th></tr>
 *   <tr><td>config_open</td><td>Shift+Alt+S</td><td>Настройки</td></tr>
 *   <tr><td>genius_lyrics</td><td>Shift+Ctrl+L</td><td>Открыть страницу на Genius</td></tr>
 *   <tr><td>history</td><td>Shift+Alt+C</td><td>История</td></tr>
 *   <tr><td>playlist_stats</td><td>Shift+Alt+A</td><td>Статистика плейлиста</td></tr>
 * </table>
 *
 * <h2>Дополнительные комбинации (sceneKeys)</h2>
 * <ul>
 *   <li>Ctrl+Alt+Shift+G — AudioHost</li>
 *   <li>Alt+Shift+I — NetworkHost</li>
 *   <li>Shift+Ctrl+Y — TrackStatistics</li>
 *   <li>Alt+Shift+S — Settings</li>
 *   <li>Alt+B — System.gc()</li>
 * </ul>
 *
 * @author Ebanina Std.
 * @version 1.0.0
 * @see HotKey
 * @see KeyBindingProcessor
 * @see ConfigurationManager
 */
@logging(tag = "hotkeys", isActive = false, fileOut = false)
public class KeyBindingController {
    /**
     * Глобальный синглтон экземпляр менеджера клавиш.
     * <p>
     * Инициализируется статически. Доступен через {@link #instance} во всём приложении.
     * Регистрируется в {@link Music#start(Stage)}.
     * </p>
     */
    public static final KeyBindingController instance = new KeyBindingController();

    /**
     * Загружает дополнительные горячие клавиши из файлов конфигурации.
     * <p>
     * Сканирует {@link ConfigurationManager#hotKeysDirectoryPath} рекурсивно ({@link FileVisitOption#FOLLOW_LINKS}).
     * Каждый файл — сериализованный {@link HotKey}. Ошибки оборачиваются в {@link RuntimeException}.
     * </p>
     * <p>Вызывается при старте приложения.</p>
     */
    public void loadBindings() {
        if(ConfigurationManager.instance.hotKeysDirectoryPath.toFile().exists()) {
            try (Stream<java.nio.file.Path> stream = Files.walk(ConfigurationManager.instance.hotKeysDirectoryPath, FOLLOW_LINKS)) {
                stream
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                hotKeys.add(HotKey.loadFromFile(file.toFile()));
                            } catch (IOException | ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Список <b>глобальных</b> горячих клавиш (работают поверх всех окон).
     * <p>
     * Инициализируется встроенными комбинациями. Дополняется из файлов через {@link #loadBindings()}.
     * Обновляется через {@link #globalKeys(NativeKeyEvent)}.
     * </p>
     */
    private final List<HotKey> hotKeys = new ArrayList<>(List.of(
            new HotKey(
                    "ebanina_skip_audio_intro_hotkey",
                    new int[] {NativeKeyEvent.VC_SHIFT, NativeKeyEvent.VC_ALT, NativeKeyEvent.VC_M},
                    () -> MediaProcessor.mediaProcessor.skipIntro(playProcessor.getTracks().get(playProcessor.getTrackIter()).getPath())
            ),
            new HotKey(
                    "ebanina_tempo_change",
                    new int[] {NativeKeyEvent.VC_SHIFT, NativeKeyEvent.VC_ALT, NativeKeyEvent.VC_PAGE_UP},
                    () -> {
                        float tempo = (float) (MediaProcessor.mediaProcessor.mediaPlayer.getTempo() + 0.05);

                        MediaProcessor.mediaProcessor.globalMap.put("tempo", tempo, float.class);
                        MediaProcessor.mediaProcessor.setTempo(tempo);
                    }
            ),
            new HotKey(
                    "ebanina_tempo_change1",
                    new int[] {NativeKeyEvent.VC_SHIFT, NativeKeyEvent.VC_ALT, NativeKeyEvent.VC_PAGE_DOWN},
                    () -> {
                        float tempo = (float) (MediaProcessor.mediaProcessor.mediaPlayer.getTempo() - 0.05);

                        MediaProcessor.mediaProcessor.globalMap.put("tempo", tempo, float.class);
                        MediaProcessor.mediaProcessor.setTempo(tempo);
                    }
            ),
            new HotKey(
                    "ebanina_next",
                    new int[] {NativeKeyEvent.VC_MEDIA_PREVIOUS},
                    () -> Platform.runLater(() -> {
                        PlayProcessor.playProcessor.down();
                    })
            ),
            new HotKey(
                    "ebanina_prev",
                    new int[] {NativeKeyEvent.VC_MEDIA_NEXT},
                    () -> Platform.runLater(() -> {
                        PlayProcessor.playProcessor.next();
                    })
            ),
            new HotKey(
                    "ebanina_skip_pit",
                    new int[] {NativeKeyEvent.VC_SHIFT, NativeKeyEvent.VC_ALT, NativeKeyEvent.VC_B},
                    () -> Platform.runLater(() -> MediaProcessor.mediaProcessor.skipPit(playProcessor.getTracks().get(playProcessor.getTrackIter()).getPath()))
            )
    ));

    /**
     * Список <b>контекстных</b> горячих клавиш (работают только при активном окне).
     * <p>
     * Обновляется через {@link #sceneKeys()}. Не загружается из файлов.
     * </p>
     */
    private final List<HotKey> sceneHotKeys = new ArrayList<>(List.of(
            new HotKey(
                    "ebanina_open_genius_lyrics_hotkey",
                    new int[] {NativeKeyEvent.VC_SHIFT, NativeKeyEvent.VC_CONTROL, NativeKeyEvent.VC_L},
                    () -> openGeniusLyrics(Root.rootImpl.currentArtist.getText() + " - " + Root.rootImpl.currentTrackName.getText())
            ),
            new HotKey(
                    "ebanina_open_history",
                    new int[] {NativeKeyEvent.VC_SHIFT, NativeKeyEvent.VC_ALT, NativeKeyEvent.VC_C},
                    () -> Platform.runLater(() -> PlayProcessor.playProcessor.getTrackHistoryGlobal().getTrackHistoryContextMenu().show(Root.rootImpl.stage.getScene().getWindow()))
            ),
            new HotKey(
                    "ebanina_set_trackListView_to_currentMusics",
                    new int[] {NativeKeyEvent.VC_SHIFT, NativeKeyEvent.VC_ALT, NativeKeyEvent.VC_Q},
                    () -> {
                        PlayProcessor.playProcessor.setTracks(Root.rootImpl.tracksListView.getTrackListView().getItems());
                    }
            ),
            new HotKey(
                    "ebanina_playback_hotkey",
                    new int[] {NativeKeyEvent.VC_SHIFT, NativeKeyEvent.VC_ALT, NativeKeyEvent.VC_P},
                    () -> MediaProcessor.mediaProcessor.getAutoPlaybackProperty().setValue(
                            MediaProcessor.mediaProcessor.getAutoPlaybackProperty().getValue()
                    )
            )
    ));

    /**
     * Возвращает список контекстных горячих клавиш.
     *
     * @return неизменяемый список {@link #sceneHotKeys}
     */
    public List<HotKey> getSceneHotKeys() {
        return sceneHotKeys;
    }

    /**
     * Возвращает список глобальных горячих клавиш.
     *
     * @return список {@link #hotKeys} (с загруженными файлами)
     */
    public List<HotKey> getHotKeys() {
        return hotKeys;
    }

    /**
     * Возвращает человекочитаемое представление комбинации клавиш.
     * <p>
     * Формат: "Shift + Alt + M". Использует {@link NativeKeyEvent#getKeyText(int)}.
     * </p>
     *
     * @param code IID горячей клавиши
     * @return строка с названиями клавиш или пустая строка
     */
    public String getHotKeysStringCodes(String code) {
        return Arrays.stream(KeyBindingController.instance.findAndGetHotKey(code).registers).mapToObj(e -> String.valueOf(NativeKeyEvent.getKeyText(e))).collect(Collectors.joining(" + "));
    }

    /**
     * Находит горячую клавишу по IID в обоих списках.
     *
     * @param code идентификатор клавиши (регистронезависимо)
     * @return {@link HotKey} или {@code null}
     */
    public HotKey findAndGetHotKey(String code) {
        return sceneHotKeys.stream().filter(e -> e.IID.equalsIgnoreCase(code)).findFirst().orElseGet(
                () -> hotKeys.stream().filter(e -> e.IID.equalsIgnoreCase(code)).findFirst().orElse(null));

    }

    /**
     * Обработчик <b>глобальных</b> клавиш (работают поверх окон).
     * <p>
     * Вызывается из {@link KeyBindingProcessor#nativeKeyPressed(NativeKeyEvent)}.
     * Проверяет {@link #hotKeys} через {@link #updateKeysOnPressedEvent(List)}.
     * </p>
     *
     * @param code событие клавиши
     */
    public void globalKeys(NativeKeyEvent code) {
        updateKeysOnPressedEvent(KeyBindingController.instance.getHotKeys());
    }

    /**
     * Обработчик <b>контекстных</b> клавиш + встроенные комбинации.
     * <p>
     * Выполняет:
     * <ul>
     *   <li>Проверку встроенных комбинаций (AudioHost, NetworkHost, статистика, GC).</li>
     *   <li>Обработку {@link #sceneHotKeys}.</li>
     * </ul>
     * Вызывается из {@link KeyBindingProcessor#setupKeyListeners(Scene)}}.
     * </p>
     */
    public void sceneKeys() {
        if(!isKeyPressed(NativeKeyEvent.VC_SHIFT) && isKeyPressed(NativeKeyEvent.VC_CONTROL) && isKeyPressed(NativeKeyEvent.VC_ALT) && isKeyPressed(NativeKeyEvent.VC_G)) {
            Platform.runLater(() -> AudioHost.instance.open(Root.rootImpl.stage));
        }

        if(isKeyPressed(NativeKeyEvent.VC_ALT) && isKeyPressed(NativeKeyEvent.VC_SHIFT) && isKeyPressed(NativeKeyEvent.VC_I)) {
            Platform.runLater(() -> {
                NetworkHost.instance.open(Root.rootImpl.stage);
            });
        }

        if(isKeyPressed(NativeKeyEvent.VC_SHIFT) && isKeyPressed(NativeKeyEvent.VC_CONTROL) && isKeyPressed(NativeKeyEvent.VC_Y)) {
            TrackStatistics.instance.open(Root.rootImpl.stage);
        }

        if(isKeyPressed(NativeKeyEvent.VC_ALT) && isKeyPressed(NativeKeyEvent.VC_SHIFT) && isKeyPressed(NativeKeyEvent.VC_S)) {
            Settings.getInstance().open(Root.rootImpl.stage);
        }

        if(isKeyPressed(NativeKeyEvent.VC_ALT) && isKeyPressed(NativeKeyEvent.VC_B) && !isKeyPressed(NativeKeyEvent.VC_SHIFT)) {
            System.gc();
        }

        updateKeysOnPressedEvent(KeyBindingController.instance.getSceneHotKeys());
    }

    /**
     * Обновляет состояние <b>нажатия</b> горячих клавиш.
     * <p>
     * Вызывает {@link HotKey#onPressed} для всех совпавших комбинаций.
     * </p>
     *
     * @param hotKeys список для проверки
     */
    protected void updateKeysOnPressedEvent(List<HotKey> hotKeys) {
        updateKeysEvent(hotKeys, (r) -> {
            if(r.onPressed != null) {
                r.onPressed.run();
            }
        });
    }

    /**
     * Обновляет состояние <b>отпускания</b> горячих клавиш.
     * <p>
     * Вызывает {@link HotKey#onReleased} для всех совпавших комбинаций.
     * </p>
     *
     * @param hotKeys список для проверки
     */
    protected void updateKeysOnReleasedEvent(List<HotKey> hotKeys) {
        updateKeysEvent(hotKeys, (r) -> {
            if(r.onReleased != null) {
                r.onReleased.run();
            }
        });
    }

    /**
     * Универсальный обработчик событий клавиш.
     * <p>
     * Алгоритм проверки:
     * <ol>
     *   <li>Для каждой {@link HotKey} из списка.</li>
     *   <li>Проверка всех клавиш {@link HotKey#registers} в {@link KeyBindingProcessor#pressed_keys}.</li>
     *   <li>Если задан {@link HotKey#getPredicate()}, проверка условия.</li>
     *   <li>При полном совпадении — вызов {@code consumer.accept(hotKey)}.</li>
     * </ol>
     * </p>
     *
     * @param hotKeys список горячих клавиш
     * @param r обработчик для активированных клавиш
     */
    protected void updateKeysEvent(List<HotKey> hotKeys, Consumer<HotKey> r) {
        // Прохождение по всем зарегистрированным горячим клавишам
        for(int pointerToKey = 0; pointerToKey < hotKeys.size(); pointerToKey++) {
            boolean isDoKeyPointer = true;

            HotKey hotKey = hotKeys.get(pointerToKey);

            // Массив из клавиш текущей горячей клавиши из списка
            int[] keys = hotKey.registers;

            // Прохождение по int[] keys
            for(int keyPointer = 0; keyPointer < keys.length; keyPointer++) {

                // Если не нужна особая обработка
                if(hotKey.getPredicate() == null) {

                    // Если нажатые клавиши не содержат текущую клавишу, то не выполнять действия
                    if (!isKeyPressed(keys[keyPointer])) {
                        isDoKeyPointer = false;

                        break;
                    }
                } else {
                    if(hotKey.getPredicate().get()) {
                        isDoKeyPointer = false;

                        break;
                    }
                }
            }

            if(isDoKeyPointer) {
                if(r != null) {
                    r.accept(hotKey);
                }
            }
        }
    }

    /**
     * Автоматически регистрирует все @KeyBind методы из переданных объектов.
     *
     * <h3>Пример:</h3>
     * <pre>{@code
     * collectKeyBindsFromObjects(
     *     this,                    // MainController
     *     MediaProcessor.mediaProcessor,
     *     PlayProcessor.playProcessor
     * );
     * }</pre>
     *
     * <p><b>Преимущества:</b></p>
     * <ul>
     *   <li>Работает с любыми объектами (singleton, DI, this)</li>
     *   <li>Без newInstance() — никаких ошибок конструкторов</li>
     *   <li>Прямой invoke(target) — максимальная скорость</li>
     * </ul>
     *
     * @param objects объекты с методами @KeyBind
     */
    public void collectKeyBindsFromObjects(Object... objects) {
        for (Object target : objects) {
            Class<?> clazz = target.getClass();

            for (Method method : clazz.getDeclaredMethods()) {
                KeyBind[] binds = method.getAnnotationsByType(KeyBind.class);

                if (binds.length == 0)
                    continue;

                method.setAccessible(true);

                Runnable action = () -> {
                    try {
                        method.invoke(target);
                    } catch (Exception e) {
                        Music.mainLogger.severe("KeyBind '" + method.getName() + "' failed: " + e.getMessage());

                        e.printStackTrace();
                    }
                };

                for (KeyBind bind : binds) {
                    HotKey hotKey = new HotKey(bind.id(), bind.keys(), action);

                    if (bind.global()) {
                        hotKeys.add(hotKey);
                    } else {
                        sceneHotKeys.add(hotKey);
                    }
                }
            }
        }
    }
}
