package rf.ebanina.ebanina.KeyBindings;

import javafx.scene.input.KeyEvent;

import java.io.*;
import java.util.function.Supplier;

/**
 * <h1>HotKey</h1>
 * Класс, представляющий комбинацию клавиш (горячие клавиши) для выполнения действий в приложении.
 * <p>
 * {@code HotKey} инкапсулирует идентификатор комбинации (IID), массив кодов клавиш ({@link #registers}),
 * обработчики нажатия ({@link #onPressed}) и отпускания ({@link #onReleased}), а также опциональные условия
 * активации — ограничение сценой ({@link #isOnlyOnScene}) и предикат ({@link #predicate}).
 * </p>
 * <p>
 * Поддерживаемые возможности:
 * <ul>
 *   <li><b>Модификаторы + клавиша действия</b> — например, Ctrl+Alt+K (массив {@code [17, 18, 75]}).</li>
 *   <li><b>Обработчики событий</b> — отдельные действия при нажатии и отпускании комбинации.</li>
 *   <li><b>Условная активация</b> — выполнение только на определённой сцене или при выполнении условия.</li>
 *   <li><b>Сериализация</b> — сохранение/загрузка настроек клавиш в файл для персистентности.</li>
 * </ul>
 * </p>
 * <p>
 * Класс предназначен для фреймворков Swing/JavaFX/AWT и не-thread-safe. Обработчики {@link Runnable}
 * вызываются в потоке обработки событий UI. Массив {@link #registers} неизменяем после создания,
 * но может быть заменён полностью через {@link #setRegisters(int[])}.
 * </p>
 * <p>
 * Класс не предназначен для наследования — все конструкторы покрывают типичные сценарии использования.
 * Fluent API через методы {@code set*()} возвращает {@code this} для цепочек вызовов.
 * </p>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * // Простая горячая клавиша Ctrl+K
 * HotKey playKey = new HotKey("play", new int[]{17, 75}, () -> System.out.println("Play!"));
 *
 * // Полная комбинация с условиями
 * HotKey saveKey = new HotKey("save", () -> saveDocument(), null,
 *     new int[]{17, 83}, true, () -> document.isModified());
 *
 * // Fluent API
 * HotKey key = new HotKey("action", new int[]{75})
 *     .setOnPressed(() -> doAction())
 *     .setOnlyOnScene(true)
 *     .setPredicate(() -> isEnabled());
 * }</pre>
 *
 * <h2>Сериализация</h2>
 * <pre>{@code
 * HotKey key = ...;
 * HotKey.saveToFile(new File("keys.dat"), key);
 * HotKey loaded = HotKey.loadFromFile(new File("keys.dat"));
 * }</pre>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li>Идентификатор IID уникален в контексте менеджера клавиш.</li>
 *   <li>Массив registers отсортирован по приоритету (модификаторы первыми).</li>
 *   <li>Предикат проверяется перед каждым вызовом обработчика.</li>
 *   <li>Пустой массив registers отключает клавишу.</li>
 * </ul>
 *
 * @author Ebanina Std.
 * @version 1.0.0
 * @since 1.2.0
 * @see Runnable
 * @see Supplier
 * @see Serializable
 */
public class HotKey
        implements Serializable
{
    /**
     * Версия сериализации для обеспечения совместимости между версиями класса.
     * <p>
     * Изменяется при внесении несовместимых изменений в структуру класса.
     * Текущее значение {@code 1L}
     * </p>
     *
     * @since 1.4.7
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Уникальный строковый идентификатор комбинации клавиш.
     * <p>
     * Используется для:
     * <ul>
     *   <li>Поиска и управления клавишами в {@code HotKeyManager}.</li>
     *   <li>Сохранения/загрузки настроек в файлы конфигурации.</li>
     *   <li>Логирования и отладки событий клавиш.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Неизменяемо</b> — устанавливается только в конструкторах. Должно быть уникальным
     * в контексте приложения. Рекомендуется использовать константы или префиксы пакетов.
     * </p>
     * <p>
     * Примеры: {@code "rf.ebanina.play"}, {@code "editor.copy"}, {@code "global.undo"}.
     * </p>
     *
     * @see #getIID()
     * @since 1.0.0
     */
    public final String IID;

    /**
     * Обработчик, вызываемый при полном нажатии комбинации клавиш.
     * <p>
     * Выполняется только если:
     * <ul>
     *   <li>Все клавиши из {@link #registers} нажаты одновременно.</li>
     *   <li>{@link #predicate} возвращает {@code true} (если задан).</li>
     *   <li>Текущая сцена соответствует {@link #isOnlyOnScene} (если ограничение активно).</li>
     * </ul>
     * </p>
     * <p>
     * Вызывается в потоке UI. Может быть {@code null} — в этом случае клавиша только отслеживается.
     * Устанавливается через конструктор или {@link #setOnPressed(Runnable)}.
     * </p>
     *
     * @see #onReleased
     * @see #predicate
     * @see #isOnlyOnScene
     * @since 1.0.0
     */
    public Runnable onPressed;

    /**
     * Обработчик, вызываемый при полном отпускании комбинации клавиш.
     * <p>
     * Выполняется после того, как <b>все</b> клавиши из {@link #registers} отпущены.
     * Условия активации те же, что и для {@link #onPressed}.
     * </p>
     * <p>
     * Полезно для:
     * <ul>
     *   <li>Завершения операций (например, остановка drag'n'drop).</li>
     *   <li>Отмены временных эффектов.</li>
     *   *   <li>Сброса флагов состояния.</li>
     * </ul>
     * </p>
     * <p>
     * Может быть {@code null}. Вызывается в потоке UI.
     * </p>
     *
     * @see #onPressed
     * @see #predicate
     * @see #isOnlyOnScene
     * @since 1.0.0
     */
    public Runnable onReleased;

    /**
     * Массив кодов клавиш, образующих комбинацию (KeyEvent.VK_* константы).
     * <p>
     * Порядок значим:
     * <ul>
     *   <li>Первые элементы — модификаторы (17=Ctrl, 16=Shift, 18=Alt).</li>
     *   <li>Последний элемент — клавиша действия.</li>
     * </ul>
     * </p>
     * <p>
     * Примеры комбинаций:
     * <ul>
     *   <li>{@code [17, 75]} — Ctrl+K</li>
     *   <li>{@code [17, 18, 83]} — Ctrl+Alt+S</li>
     *   <li>{@code [75]} — просто K</li>
     * </ul>
     * </p>
     * <p>
     * <b>Особенности:</b>
     * <ul>
     *   <li>Ссылка неизменяема, но содержимое может быть заменено через {@link #setRegisters(int[])}.</li>
     *   <li>Пустой массив отключает клавишу.</li>
     *   <li>Дубликаты игнорируются при обработке.</li>
     * </ul>
     * </p>
     *
     * @see #setRegisters(int[])
     * @see KeyEvent
     * @since 1.0.0
     */
    public int[] registers;

    /**
     * Флаг ограничения активации клавиши только активной сценой/экрана.
     * <p>
     * Если {@code true}, клавиша обрабатывается только когда её IID соответствует текущей сцене.
     * Полезно для контекстно-зависимых горячих клавиш (например, редактор vs плеер).
     * </p>
     * <p>
     * Логика проверки обычно реализуется в {@code HotKeyManager}:
     * <pre>
     * if (!key.isOnlyOnScene || currentSceneId.equals(key.IID))
     *     key.onPressed.run();
     * </pre>
     * </p>
     * <p>
     * По умолчанию {@code false} — клавиша глобальна.
     * </p>
     *
     * @see #IID
     * @see #setOnlyOnScene(boolean)
     * @since 1.0.0
     */
    private boolean isOnlyOnScene = false;

    /**
     * Опциональное условие активации клавиши.
     * <p>
     * Вызывается перед каждым потенциальным выполнением {@link #onPressed} или {@link #onReleased}.
     * Если возвращает {@code false}, обработчик не вызывается.
     * </p>
     * <p>
     * Примеры предикатов:
     * <ul>
     *   <li>{@code () -> document.isModified()}</li>
     *   <li>{@code () -> player.isPlaying()}</li>
     *   <li>{@code () -> user.hasPermission("save")}</li>
     * </ul>
     * </p>
     * <p>
     * Может быть {@code null} — в этом случае условие всегда истинно.
     * Вызывается в потоке UI.
     * </p>
     *
     * @see #setPredicate(Supplier)
     * @see #getPredicate()
     * @since 1.0.0
     */
    private Supplier<Boolean> predicate;

    /**
     * Полный конструктор со всеми параметрами.
     * <p>
     * Создаёт полностью настроенную горячую клавишу с обработчиками, условиями и ограничениями.
     * </p>
     *
     * @param IID идентификатор комбинации; не должен быть {@code null}
     * @param onPressed обработчик нажатия; может быть {@code null}
     * @param onReleased обработчик отпускания; может быть {@code null}
     * @param registers массив кодов клавиш; не должен быть {@code null}
     * @param isOnlyOnScene ограничение сценой
     * @param predicate условие активации; может быть {@code null}
     * @throws NullPointerException если IID или registers равны {@code null}
     */
    public HotKey(String IID, Runnable onPressed, Runnable onReleased, int[] registers, boolean isOnlyOnScene, Supplier<Boolean> predicate) {
        this.IID = IID;
        this.onPressed = onPressed;
        this.onReleased = onReleased;
        this.registers = registers;
        this.isOnlyOnScene = isOnlyOnScene;
        this.predicate = predicate;
    }

    /**
     * Базовый конструктор: IID + клавиши + обработчик нажатия.
     * <p>
     * Создаёт простую глобальную клавишу без дополнительных условий.
     * </p>
     *
     * @param iid идентификатор
     * @param registers коды клавиш
     * @param onPressed обработчик
     */
    public HotKey(String iid, int[] registers, Runnable onPressed) {
        IID = iid;
        this.onPressed = onPressed;
        this.registers = registers;
    }

    /**
     * Конструктор с ограничением сцены.
     *
     * @param IID идентификатор
     * @param isOnlyOnScene ограничение сценой
     * @param registers коды клавиш
     * @param onPressed обработчик
     */
    public HotKey(String IID, boolean isOnlyOnScene, int[] registers, Runnable onPressed) {
        this.IID = IID;
        this.onPressed = onPressed;
        this.registers = registers;
        this.isOnlyOnScene = isOnlyOnScene;
    }

    /**
     * Конструктор с обработчиками нажатия и отпускания.
     *
     * @param IID идентификатор
     * @param registers коды клавиш
     * @param onPressed обработчик нажатия
     * @param onReleased обработчик отпускания
     */
    public HotKey(String IID, int[] registers, Runnable onPressed, Runnable onReleased) {
        this.IID = IID;
        this.onPressed = onPressed;
        this.onReleased = onReleased;
        this.registers = registers;
    }

    /**
     * Минимальный конструктор: только идентификатор и клавиши.
     * <p>
     * Для создания "заготовки", которую потом донастроят через fluent API.
     * </p>
     *
     * @param ebaninaPlayHotkey идентификатор
     * @param ints коды клавиш
     */
    public HotKey(String ebaninaPlayHotkey, int[] ints) {
        IID = ebaninaPlayHotkey;
        this.registers = ints;
    }

    /**
     * Возвращает уникальный идентификатор комбинации.
     *
     * @return IID; никогда не {@code null}
     */
    public String getIID() {
        return IID;
    }

    /**
     * Возвращает обработчик нажатия.
     *
     * @return обработчик или {@code null}
     */
    public Runnable getOnPressed() {
        return onPressed;
    }

    /**
     * Устанавливает обработчик нажатия. Возвращает {@code this} для fluent API.
     *
     * @param onPressed новый обработчик; может быть {@code null}
     * @return этот объект
     */
    public HotKey setOnPressed(Runnable onPressed) {
        this.onPressed = onPressed;
        return this;
    }

    /**
     * Возвращает обработчик отпускания.
     *
     * @return обработчик или {@code null}
     */
    public Runnable getOnReleased() {
        return onReleased;
    }

    /**
     * Устанавливает обработчик отпускания. Возвращает {@code this}.
     *
     * @param onReleased новый обработчик
     * @return этот объект
     */
    public HotKey setOnReleased(Runnable onReleased) {
        this.onReleased = onReleased;
        return this;
    }

    /**
     * Заменяет массив кодов клавиш. Возвращает {@code this}.
     *
     * @param registers новый массив; не должен быть {@code null}
     * @return этот объект
     * @throws NullPointerException если registers равен {@code null}
     */
    public HotKey setRegisters(int[] registers) {
        this.registers = registers;
        return this;
    }

    /**
     * Возвращает текущие коды клавиш.
     *
     * @return массив кодов; никогда не {@code null} после создания
     */
    public int[] getRegisters() {
        return registers;
    }

    /**
     * Проверяет ограничение активации сценой.
     *
     * @return {@code true}, если клавиша активна только на своей сцене
     */
    public boolean isOnlyOnScene() {
        return isOnlyOnScene;
    }

    /**
     * Устанавливает ограничение сцены. Возвращает {@code this}.
     *
     * @param onlyOnScene новое значение
     * @return этот объект
     */
    public HotKey setOnlyOnScene(boolean onlyOnScene) {
        isOnlyOnScene = onlyOnScene;
        return this;
    }

    /**
     * Устанавливает предикат активации. Возвращает {@code this}.
     *
     * @param predicate новое условие; может быть {@code null}
     * @return этот объект
     */
    public HotKey setPredicate(Supplier<Boolean> predicate) {
        this.predicate = predicate;
        return this;
    }

    /**
     * Возвращает предикат активации.
     *
     * @return условие или {@code null}
     */
    public Supplier<Boolean> getPredicate() {
        return predicate;
    }

    /**
     * Сохраняет объект {@code HotKey} в файл.
     * <p>
     * Использует стандартную Java-сериализацию. Обработчики {@link Runnable} должны быть
     * сериализуемыми, иначе возникнет {@link NotSerializableException}.
     * </p>
     *
     * @param file целевой файл
     * @param key клавиша для сохранения; не должна быть {@code null}
     * @throws IOException при ошибках записи
     * @throws NullPointerException если file или key равны {@code null}
     */
    public static void saveToFile(File file, HotKey key) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(key);
        }
    }

    /**
     * Загружает объект {@code HotKey} из файла.
     *
     * @param file исходный файл
     * @return восстановленная клавиша
     * @throws IOException при ошибках чтения
     * @throws ClassNotFoundException если класс не найден
     * @throws InvalidClassException если файл повреждён
     */
    public static HotKey loadFromFile(File file) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (HotKey) ois.readObject();
        }
    }
}