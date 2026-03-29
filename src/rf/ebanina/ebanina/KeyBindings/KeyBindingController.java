package rf.ebanina.ebanina.KeyBindings;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelListener;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <h1>KeyBindingController</h1>
 * Центральный контроллер глобальных горячих клавиш и событий мыши через нативные хуки JNativeHook.
 * <p>
 * {@code KeyBindingController} реализует три интерфейса слушателей:
 * <ul>
 *   <li>{@link NativeKeyListener} — глобальные клавиатурные события.</li>
 *   <li>{@link NativeMouseInputListener} — клики мыши с модификаторами.</li>
 *   <li>{@link NativeMouseWheelListener} — прокрутка колеса с модификаторами.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Основная логика:</b>
 * <ul>
 *   <li>Отслеживание <b>нажатых клавиш</b> в {@link #pressed_keys} для определения комбинаций.</li>
 *   <li>Обработка <b>глобальных горячих клавиш</b> через {@link Keys#globalKeys(NativeKeyEvent)}.</li>
 *   <li><b>Колёсико мыши:</b> Alt+Shift+Wheel изменяет темп воспроизведения ({@link MediaProcessor#setTempo}).</li>
 *   <li><b>Боковые кнопки мыши:</b> Alt+Mouse4/5 — навигация по истории ({@link PlayProcessor#getTrackHistoryGlobal()}).</li>
 * </ul>
 * </p>
 * <p>
 * Класс <b>thread-safe</b>: {@link #pressed_keys} — синхронизированный {@code HashSet}.
 * Регистрируется в {@link Music} через {@link GlobalScreen}.
 * </p>
 *
 * <h2>Комбинации клавиш</h2>
 * <table>
 *   <tr><th>Комбинация</th><th>Действие</th></tr>
 *   <tr><td>Alt+Shift+Wheel</td><td>Изменение темпа (±5% за шаг)</td></tr>
 *   <tr><td>Alt+Mouse4</td><td>Предыдущий трек из истории</td></tr>
 *   <tr><td>Alt+Mouse5</td><td>Следующий трек из истории</td></tr>
 * </table>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * // Регистрация (автоматически в Music.start())
 * GlobalScreen.addNativeKeyListener(new KeyBindingController());
 * GlobalScreen.addNativeMouseListener(new KeyBindingController());
 * GlobalScreen.addNativeMouseWheelListener(new KeyBindingController());
 *
 * // Проверка нажатия клавиши
 * if (KeyBindingController.isKeyPressed(NativeKeyEvent.VC_CONTROL)) { ... }
 *
 * // JavaFX-сцена
 * KeyBindingController.setupKeyListeners(scene);
 * }</pre>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li><b>Глобальность</b> — работает поверх всех окон (включая свёрнутые).</li>
 *   <li><b>Состояние клавиш</b> — отслеживается до полного отпускания всех клавиш.</li>
 *   <li><b>Двойная обработка</b> — нативные хуки + JavaFX-события сцены.</li>
 *   <li><b>Автоочистка</b> — {@link #pressed_keys} очищается при полном отпускании.</li>
 * </ul>
 *
 * @author Ebanina Std.
 * @version 1.0.1
 * @see Keys
 * @see HotKey
 * @see GlobalScreen
 * @see MediaProcessor
 * @see PlayProcessor
 */
public final class KeyBindingController
        implements NativeMouseInputListener, NativeMouseWheelListener, NativeKeyListener
{
    /**
     * Синхронизированный набор кодов <b>нажатых клавиш</b>.
     * <p>
     * Используется для определения активных комбинаций в реальном времени.
     * <ul>
     *   <li>Добавляется при {@link #nativeKeyPressed(NativeKeyEvent)} и JavaFX KEY_PRESSED.</li>
     *   <li>Очищается при полном отпускании ({@link #nativeKeyReleased(NativeKeyEvent)}).</li>
     * </ul>
     * Thread-safe через {@link Collections#synchronizedSet(Set)}}
     * </p>
     */
    static Set<Integer> pressed_keys = Collections.synchronizedSet(new HashSet<>());

    /**
     * Проверяет, нажата ли клавиша с заданным кодом.
     * <p>
     * Быстрая проверка наличия в {@link #pressed_keys} (O(1) для HashSet).
     * </p>
     *
     * @param code код клавиши (NativeKeyEvent.VC_* или KeyCode.getCode())
     * @return {@code true}, если клавиша нажата
     */
    public static boolean isKeyPressed(int code) {
        return pressed_keys.contains(code);
    }

    /**
     * Обработка прокрутки колеса мыши.
     * <p>
     * <b>Alt+Shift+Wheel</b> изменяет темп воспроизведения на ±5% за шаг.
     * Направление инвертировано: прокрутка вверх ускоряет, вниз замедляет.
     * </p>
     *
     * @param e событие колеса мыши
     * @see MediaProcessor#setTempo(float)
     */
    @Override
    public void nativeMouseWheelMoved(NativeMouseWheelEvent e) {
        if(isKeyPressed(NativeKeyEvent.VC_ALT) && isKeyPressed(NativeKeyEvent.VC_SHIFT)) {
            MediaProcessor.mediaProcessor.setTempo((float) (MediaProcessor.mediaProcessor.mediaPlayer.getTempo() + (-e.getWheelRotation()) * 0.05));
        }
    }

    /**
     * Обработка отпускания кнопок мыши.
     * <p>
     * <b>Alt+Mouse4/5</b> — навигация по истории воспроизведения:
     * <ul>
     *   <li>Mouse4 (forward) — предыдущий трек.</li>
     *   <li>Mouse5 (back) — следующий трек.</li>
     * </ul>
     * </p>
     *
     * @param e событие мыши
     * @see PlayProcessor#getTrackHistoryGlobal()
     * @see Root.PlaylistHandler#openTrack(Track)
     */
    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        if((e.getButton() == 4) && isKeyPressed(NativeKeyEvent.VC_ALT)) {
            Root.PlaylistHandler.openTrack(PlayProcessor.playProcessor.getTrackHistoryGlobal().back());
        }

        if((e.getButton() == 5) && isKeyPressed(NativeKeyEvent.VC_ALT)) {
            Root.PlaylistHandler.openTrack(PlayProcessor.playProcessor.getTrackHistoryGlobal().forward());
        }
    }

    /**
     * Обработка отпускания клавиши.
     * <p>
     * Выполняет:
     * <ul>
     *   <li>Обновление состояния {@link Keys} ({@link Keys#updateKeysOnReleasedEvent}).</li>
     *   <li>Полную очистку {@link #pressed_keys} при отсутствии нажатых клавиш.</li>
     * </ul>
     * </p>
     *
     * @param e событие клавиши
     */
    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        Keys.instance.updateKeysOnReleasedEvent(Keys.instance.getHotKeys());

        if(pressed_keys.size() != 0) {
            pressed_keys.clear();
        }
    }

    /**
     * Обработка нажатия клавиши.
     * <p>
     * Последовательность:
     * <ol>
     *   <li>Добавление кода в {@link #pressed_keys}.</li>
     *   <li>Проверка глобальных горячих клавиш {@link Keys#globalKeys(NativeKeyEvent)}.</li>
     * </ol>
     * </p>
     *
     * @param e событие клавиши
     */
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        pressed_keys.add(e.getKeyCode());

        Keys.instance.globalKeys(e);
    }

    /**
     * Регистрирует слушатели клавиш.
     * <p>
     * Дополняет нативные хуки локальной обработкой:
     * <ul>
     *   <li>KEY_PRESSED — добавление в {@link #pressed_keys} + {@link Keys#sceneKeys()}.</li>
     *   <li>KEY_RELEASED — очистка состояния (дублирует нативную логику).</li>
     * </ul>
     * Вызывается в {@link Music#start(Stage)}.
     * </p>
     *
     * @param scene JavaFX-сцена для регистрации
     */
    public static void setupKeyListeners(Scene scene) {
        // Хуйня
        KeyBindingController c = new KeyBindingController();

        // Регистрация нативного кода, для отлова горячих клавиш
        try {
            GlobalScreen.registerNativeHook();

            Music.mainLogger.info("GlobalScreen native hook registered successfully");
        } catch (NativeHookException ex) {
            Music.mainLogger.err(ex);

            Music.mainLogger.severe("Failed to register GlobalScreen hook: %s", ex.getMessage());
        }

        Music.mainLogger.info("KeyBindingController initialized and listeners registered");

        // Загрузка собственных горячих клавиш
        Keys.instance.loadBindings();

        // Загрузка дефолтных горячих клавиш
        Keys.instance.collectKeyBindsFromObjects(
                MediaProcessor.mediaProcessor
        );

        // Инициализация слушателей горячих клавиш
        GlobalScreen.addNativeKeyListener(c);
        GlobalScreen.addNativeMouseListener(c);
        GlobalScreen.addNativeMouseWheelListener(c);

        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            pressed_keys.add(event.getCode().getCode());
            Keys.instance.sceneKeys();
        });

        scene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            pressed_keys.add(event.getCode().getCode());
        });

    }
}


