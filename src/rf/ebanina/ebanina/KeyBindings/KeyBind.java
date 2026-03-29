package rf.ebanina.ebanina.KeyBindings;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import java.lang.annotation.*;

/**
 * Аннотация для регистрации горячих клавиш в системе обработки клавиатурных событий.
 * <p>
 * Применяется к методам без параметров, которые будут автоматически вызываться при нажатии
 * заданной комбинации клавиш. Методы сканируются через рефлексию при инициализации и
 * добавляются в соответствующие списки глобальных ({@code hotKeys}) или контекстных
 * ({@code sceneHotKeys}) горячих клавиш.
 * </p>
 *
 * <h3>Особенность множественности:</h3>
 * <p>Поддерживает <b>несколько аннотаций на одном методе</b> для разных комбинаций:</p>
 * <pre>{@code
 * @KeyBind(id = "play_media", keys = {NativeKeyEvent.VC_MEDIA_PLAY}, global = true)
 * @KeyBind(id = "play_numlock", keys = {NativeKeyEvent.VC_NUM_LOCK}, global = true)
 * public void pause_play() { ... }
 * }</pre>
 *
 * <h3>Применение:</h3>
 * <pre>{@code
 * @KeyBind(
 *     id = "ebanina_open_history",
 *     keys = {NativeKeyEvent.VC_SHIFT, NativeKeyEvent.VC_ALT, NativeKeyEvent.VC_C}
 * )
 * public void openHistory() {
 *     Platform.runLater(() -> PlayProcessor.playProcessor.getTrackHistoryGlobal()
 *         .getTrackHistoryContextMenu().show(stage.getScene().getWindow()));
 * }
 * }</pre>
 *
 * <h3>Элементы аннотации:</h3>
 * <ul>
 *   <li>{@link #id()} - уникальный ID для конфигурации и логирования</li>
 *   <li>{@link #keys()} - массив кодов клавиш из {@link NativeKeyEvent}</li>
 *   <li>{@link #global()} - тип: {@code true} = глобальная, {@code false} = контекстная</li>
 * </ul>
 *
 * <h3>Обработка:</h3>
 * <ul>
 *   <li><b>Глобальные</b> (global=true): добавляются в список {@code hotKeys},
 *       обрабатываются методом {@code globalKeys(NativeKeyEvent)}</li>
 *   <li><b>Контекстные</b> (global=false): добавляются в {@code sceneHotKeys},
 *       обрабатываются методом {@code sceneKeys()}</li>
 *   <li>Сбор выполняется методом {@code collectKeyBindsFromAnnotations()} при инициализации</li>
 * </ul>
 *
 * <h3>Требования к методам:</h3>
 * <ul>
 *   <li>Без параметров (void-returning или игнорируется)</li>
 *   <li>Доступны через рефлексию ({@code public} или {@code setAccessible(true)})</li>
 * </ul>
 *
 * <h3>Использование в коде:</h3>
 * <pre>{@code
 * // В инициализации:
 * collectKeyBindsFromAnnotations();
 * loadBindings();  // Дополнение из файлов по ID
 * }</pre>
 *
 * @implNote Каждая аннотация создает отдельный {@link HotKey} объект
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(KeyBinds.class)
@Target(ElementType.METHOD)
public @interface KeyBind {

    /**
     * Уникальный идентификатор горячей клавиши.
     *
     * <p><b>Назначение:</b></p>
     * <ul>
     *   <li>Сохранение/загрузка комбинаций из файлов конфигурации ({@code loadBindings()})</li>
     *   <li>Поиск и переопределение клавиш по ID</li>
     *   <li>Логирование событий и отладка</li>
     * </ul>
     *
     * <p><b>Примеры:</b></p>
     * <pre>"ebanina_open_history", "ebanina_play_hotkey", "ebanina_skip_intro"</pre>
     *
     * @return уникальное имя (не пустое)
     */
    String id();

    /**
     * Массив кодов клавиш для комбинации.
     *
     * <p>Коды из {@link NativeKeyEvent} констант (VC_*).
     * Порядок: модификаторы первыми (Shift/Ctrl/Alt), затем основная клавиша.</p>
     *
     * <p><b>Примеры:</b></p>
     * <pre>
     * Shift+Alt+C:   {VC_SHIFT, VC_ALT, VC_C}
     * Num Lock:      {VC_NUM_LOCK}
     * Media Play:    {VC_MEDIA_PLAY}
     * </pre>
     *
     * @return непустой массив кодов клавиш
     */
    int[] keys();

    /**
     * Тип горячей клавиши.
     *
     * <dl>
     *   <dt>{@code true}</dt><dd>Глобальная: работает поверх всех окон, список {@code hotKeys}</dd>
     *   <dt>{@code false}</dt><dd>Контекстная: только активное окно, список {@code sceneHotKeys}</dd>
     * </dl>
     *
     * <p><b>По умолчанию:</b> {@code false} (контекстная)</p>
     *
     * @return true для глобальной клавиши
     */
    boolean global() default false;
}