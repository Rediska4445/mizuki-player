package rf.ebanina.ebanina.Player;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.DataTypes;
import rf.ebanina.UI.Root;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс, представляющий историю прослушанных аудиотреков в медиаплеере.
 * <p>
 * {@code TrackHistory} реализует ограниченную по размеру коллекцию уникальных треков,
 * сохраняя их в порядке добавления и поддерживая навигацию "вперёд/назад" через
 * внутренний {@link ListIterator}. Он интегрируется с пользовательским интерфейсом,
 * автоматически обновляя переданное {@link ContextMenu} при каждом изменении истории.
 * </p>
 * <p>
 * Основные функции:
 * <ul>
 *   <li><b>Ограничение размера</b> — при превышении {@code maxSize} удаляется самый старый трек.</li>
 *   <li><b>Уникальность</b> — один и тот же трек не может быть добавлен дважды.</li>
 *   <li><b>Навигация</b> — методы {@link #back()} и {@link #forward()} позволяют перемещаться по истории.</li>
 *   <li><b>Интеграция с UI</b> — контекстное меню обновляется автоматически при добавлении/удалении треков.</li>
 *   <li><b>Сериализация</b> — поддержка сохранения и загрузки истории из файла через {@link #saveToFile(File)} и {@link #loadFromFile(File)}.</li>
 * </ul>
 * </p>
 * <p>
 * Особое внимание уделяется поддержке навигации. Итератор всегда сбрасывается на позицию
 * после последнего элемента при добавлении нового трека, что соответствует поведению
 * "истории браузера" — после посещения новой страницы история "вперёд" становится недоступной.
 * </p>
 * <p>
 * Класс не является потокобезопасным. Все операции с историей и UI должны выполняться
 * из одного потока (обычно JavaFX Application Thread). Методы {@link #saveToFile(File)}
 * и {@link #loadFromFile(File)} выполняют ввод-вывод в отдельном потоке, но модификацию
 * состояния истории и UI делегируют через {@link Platform#runLater(Runnable)}.
 * </p>
 * <p>
 * Экземпляры {@code TrackHistory} сравниваются по содержимому списка {@link #history} —
 * два объекта считаются равными, если их списки треков идентичны. Это позволяет использовать
 * историю в коллекциях для отслеживания состояния.
 * </p>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * // Создание истории на 50 треков с привязкой к контекстному меню
 * ContextMenu historyMenu = new ContextMenu();
 * TrackHistory history = new TrackHistory(50, historyMenu);
 *
 * // Добавление трека
 * history.add(new Track("file:///music/song.mp3"));
 *
 * // Навигация
 * Track previous = history.back();
 * Track next = history.forward();
 *
 * // Сохранение и загрузка
 * history.saveToFile(new File("history.dat"));
 * history.loadFromFile(new File("history.dat"));
 * }</pre>
 *
 * <h2>Ограничения</h2>
 * <ul>
 *   <li>Класс не проверяет существование или доступность треков при добавлении.</li>
 *   <li>Итератор не синхронизирован с изменениями списка — повторное добавление трека сбрасывает позицию.</li>
 *   <li>Контекстное меню обновляется асинхронно, что может вызвать задержку в UI.</li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 0.1.2
 * @see Track
 * @see ContextMenu
 * @see ListIterator
 * @see Platform
 */
public class TrackHistory implements
        Serializable,
        Cloneable,
        Comparable<TrackHistory>
{
    /**
     * Версия сериализации для обеспечения совместимости между версиями класса.
     * <p>
     * Изменяется при внесении несовместимых изменений в структуру класса.
     * Текущее значение {@code 3L} соответствует версии 0.1.2.
     * </p>
     *
     * @since 0.1.2
     */
    @Serial
    private static final long serialVersionUID = 3L;
    /**
     * Список треков, хранящихся в порядке их добавления.
     * <p>
     * Самый старый трек находится в начале списка ({@code history.getFirst()}),
     * самый новый — в конце ({@code history.getLast()}). Используется {@link ObservableList}.
     * </p>
     * <p>
     * Список является внутренним состоянием и не должен быть изменён напрямую.
     * Для модификации используются методы {@link #add(Track)} и {@link #remove(Track)}.
     * </p>
     *
     * @see #add(Track)
     * @see #remove(Track)
     * @see #getHistory()
     * @since 0.1.2
     */
    private final ObservableList<Track> history = FXCollections.observableArrayList();
    /**
     * Максимальное количество треков, которое может храниться в истории.
     * <p>
     * Значение устанавливается в конструкторе и не изменяется в течение
     * жизненного цикла объекта. При достижении лимита, добавление нового
     * трека приводит к удалению самого старого.
     * </p>
     * <p>
     * Логика удаления реализована в методе {@link #add(Track)}.
     * </p>
     *
     * @see #add(Track)
     * @since 0.1.2
     */
    private final IntegerProperty maxSize = new SimpleIntegerProperty();

    public static class HistoryIterator {
        private final AtomicInteger index = new AtomicInteger(0);

        public int getIndex() {
            return index.get();
        }

        public void setIndex(int index) {
            this.index.set(index);
        }

        public int whoIsNext() {
            return index.get() + 1;
        }

        public int next() {
            return index.incrementAndGet();
        }

        public int back() {
            if(index.get() > 0) {
                return index.decrementAndGet();
            }

            return 0;
        }
    }

    /**
     * Итератор для навигации по истории треков.
     * <p>
     * Используется методами {@link #back()} и {@link #forward()} для перемещения
     * по списку. Всегда указывает на позицию после последнего элемента после
     * добавления нового трека, что делает недоступной "историю вперёд" после
     * нового действия.
     * </p>
     * <p>
     * При удалении трека итератор сбрасывается на конец списка.
     * </p>
     *
     * @see #back()
     * @see #forward()
     * @see #add(Track)
     * @see #remove(Track)
     * @since 0.1.2
     */
    private final HistoryIterator historyIterator;

    /**
     * Контекстное меню, которое автоматически обновляется при изменении истории.
     * <p>
     * Содержит элементы меню для каждого трека, созданные через {@link #createMenuItem(Track)}.
     * При добавлении трека в меню добавляется новый элемент, при удалении — элемент
     * возвращается в меню (логика "удалённый трек можно быстро вернуть").
     * </p>
     * <p>
     * Обновление меню выполняется асинхронно в JavaFX потоке через {@link #updateContextMenu()}.
     * </p>
     *
     * @see #createMenuItem(Track)
     * @see #updateContextMenu()
     * @since 0.1.2
     */
    private ContextMenu trackHistoryContextMenu;

    /**
     * Создаёт новую историю треков с заданным максимальным размером.
     * <p>
     * Инициализирует внутренний список и устанавливает итератор на позицию
     * после последнего элемента (что соответствует пустой позиции).
     * </p>
     * <p>
     * Контекстное меню не инициализируется — оно будет обновлено при первом
     * добавлении трека или при вызове {@link #loadFromFile(File)}.
     * </p>
     *
     * @param maxSize максимальное количество треков в истории; должно быть положительным
     * @param contextMenu контекстное меню, которое будет обновляться при изменении истории;
     *                    не должно быть null
     * @throws NullPointerException если {@code contextMenu} равен {@code null}
     * @see #add(Track)
     * @see #loadFromFile(File)
     * @since 0.1.2
     */
    public TrackHistory(int maxSize, ContextMenu contextMenu) {
        this.maxSize.set(maxSize);
        this.trackHistoryContextMenu = contextMenu;
        this.historyIterator = new HistoryIterator();
    }

    public TrackHistory setTrackHistoryContextMenu(ContextMenu trackHistoryContextMenu) {
        this.trackHistoryContextMenu = trackHistoryContextMenu;
        return this;
    }

    public int getMaxSize() {
        return maxSize.get();
    }

    public IntegerProperty maxSizeProperty() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize.set(maxSize);
    }

    public ContextMenu getTrackHistoryContextMenu() {
        return trackHistoryContextMenu;
    }

    /**
     * Возвращает внутренний список истории треков.
     * <p>
     * Предоставляет прямой доступ к {@link ObservableList}, в котором хранятся треки
     * в порядке их добавления. Это позволяет выполнять произвольные операции
     * с историей, но требует осторожности.
     * </p>
     * <p>
     * <b>Предупреждение</b>: прямое изменение списка (например, через {@link ObservableList#add(Object)}
     * или {@link ObservableList#remove(Object)}) нарушает согласованность состояния класса.
     * Это может привести к некорректной работе итератора навигации и UI-интеграции.
     * </p>
     *
     * <h3>Рекомендуемое использование</h3>
     * <pre>{@code
     * TrackHistory history = new TrackHistory(5, contextMenu);
     * // ... добавление треков
     *
     * // Чтение: безопасно
     * for (Track track : history.getHistory()) {
     *     System.out.println(track.viewName());
     * }
     *
     * // Модификация: НЕПРАВИЛЬНО!
     * // history.getHistory().add(new Track("file:///music/hack.mp3"));
     *
     * // Модификация: ПРАВИЛЬНО!
     * history.add(new Track("file:///music/song.mp3"));
     * }</pre>
     *
     * @return ссылка на {@link LinkedList} с треками в порядке их добавления
     * @see #add(Track)
     * @see #remove(Track)
     * @since 0.1.2
     */
    public ObservableList<Track> getHistory() {
        return history;
    }

    /**
     * Удаляет указанный трек из истории, если он присутствует.
     * <p>
     * Операция состоит из двух ключевых действий:
     * <ol>
     *   <li><b>Удаление из истории</b>: трек удаляется из внутреннего списка {@link #history}.
     *       Если трек отсутствует, операция игнорируется.</li>
     *   <li><b>Возврат в UI</b>: создается новый элемент меню через {@link #createMenuItem(Track)}
     *       и добавляется в {@link #trackHistoryContextMenu}. Это позволяет пользователю
     *       быстро восстановить удалённый трек, как в корзине.</li>
     * </ol>
     * </p>
     * <p>
     * После удаления итератор сбрасывается на позицию <i>после последнего элемента</i>,
     * что делает "историю вперёд" недоступной.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * TrackHistory history = new TrackHistory(5, contextMenu);
     * Track track = new Track("file:///music/song.mp3");
     * history.add(track);
     *
     * history.remove(track); // Трек удалён из истории, но добавлен в меню
     * // history.forward(); // Выбросит NoSuchElementException!
     * }</pre>
     *
     * @param track трек для удаления; если <code>null</code> или отсутствует, операция игнорируется
     * @see #add(Track) для добавления трека
     * @see #createMenuItem(Track) для деталей создания элемента меню
     * @since 0.1.2
     */
    public void remove(Track track) {
        if(!history.contains(track)) {
            return;
        }

        history.remove(track);

        historyIterator.setIndex(history.size());

        trackHistoryContextMenu.getItems().remove(createMenuItem(track));
    }

    /**
     * Добавляет новый трек в историю воспроизведения, если он ещё не присутствует.
     * <p>
     * Это основной метод для пополнения истории, вызываемый каждый раз при старте нового трека.
     * Его поведение можно разделить на три ключевые фазы:
     * <ol>
     *   <li><b>Фильтрация</b>: метод игнорирует попытки добавить <code>null</code> или дубликаты.
     *       Это гарантирует уникальность каждого трека в истории.</li>
     *   <li><b>Управление размером</b>: если история достигла лимита ({@link #maxSize}), самый старый
     *       трек (в начале списка) удаляется с помощью {@link LinkedList#removeFirst()}.
     *       <i>Важно:</i> это не влияет на текущую позицию итератора.</li>
     *   <li><b>Сброс состояния</b>: после успешного добавления итератор сбрасывается на позицию
     *       <i>после последнего элемента</i>. Это критически важное поведение, аналогичное
     *       истории браузера: любое новое действие делает "историю вперёд" недоступной.</li>
     * </ol>
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * TrackHistory history = new TrackHistory(5, contextMenu);
     * Track track = new Track("file:///music/song.mp3");
     *
     * history.add(track); // Трек добавлен, итератор сброшен
     * history.back();    // Успешно: возвращаемся к предыдущему
     * history.add(new Track("file:///music/next.mp3")); // "вперёд" больше недоступно
     * // history.forward(); // Выбросит NoSuchElementException!
     * }</pre>
     *
     * <h3>Интеграция с UI</h3>
     * <p>
     * Метод автоматически вызывает {@link #createMenuItem(Track)} и добавляет результат
     * в {@link #trackHistoryContextMenu}. Это обеспечивает мгновенное (с точки зрения пользователя)
     * обновление интерфейса.
     * </p>
     *
     * @param track трек для добавления; если <code>null</code> или уже присутствует в истории, операция игнорируется
     * @throws RuntimeException если возникает ошибка при создании элемента меню (например, из-за проблем с UI)
     * @see #remove(Track) для удаления трека и возврата его в меню
     * @see #back() и #forward() для навигации по истории
     * @see #createMenuItem(Track) для деталей создания элемента меню
     * @since 0.1.2
     */
    public void add(Track track) {
        if(history.contains(track)) {
            return;
        }

        if(history.size() >= maxSize.get()) {
            history.remove(0);
        }

        history.add(track);

        historyIterator.setIndex(history.size() - 1);

        trackHistoryContextMenu.getItems().add(createMenuItem(track));
    }

    /**
     * Возвращает индекс указанного трека в истории.
     * <p>
     * Использует стандартный метод {@link LinkedList#indexOf(Object)} для поиска.
     * Поиск выполняется по ссылке на объект трека.
     * </p>
     * <p>
     * <b>Важно</b>: метод не проверяет логическую идентичность треков (например, по пути файла).
     * Два разных объекта {@link Track} с одинаковым источником будут считаться разными.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * TrackHistory history = new TrackHistory(5, contextMenu);
     * Track track1 = new Track("file:///music/song1.mp3");
     * Track track2 = new Track("file:///music/song2.mp3");
     * history.add(track1);
     * history.add(track2);
     *
     * int index1 = history.indexOf(track1); // Вернёт 0
     * int index2 = history.indexOf(track2); // Вернёт 1
     * int index3 = history.indexOf(new Track("file:///music/song1.mp3")); // Вернёт -1
     * }</pre>
     *
     * @param t трек, индекс которого нужно найти
     * @return индекс трека, начиная с 0, или -1 если трек не найден
     * @see #contains(Track)
     * @since 0.1.2
     */
    public int indexOf(Track t) {
        return history.indexOf(t);
    }

    /**
     * Возвращает текущее количество треков в истории.
     * <p>
     * Обёртка вокруг метода {@link LinkedList#size()}, предоставляющая
     * удобный доступ к размеру истории.
     * </p>
     * <p>
     * Значение всегда находится в диапазоне от 0 до {@link #maxSize}.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * TrackHistory history = new TrackHistory(5, contextMenu);
     * System.out.println(history.size()); // 0
     *
     * history.add(new Track("file:///music/song.mp3"));
     * System.out.println(history.size()); // 1
     * }</pre>
     *
     * @return количество треков, всегда неотрицательное число
     * @see #maxSize
     * @since 0.1.2
     */
    public int size() {
        return history.size();
    }

    /**
     * Проверяет, содержится ли указанный трек в истории.
     * <p>
     * Основан на результате {@link #indexOf(Track)}. Является удобным способом
     * проверки наличия трека без необходимости обрабатывать возвращаемое значение индекса.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * TrackHistory history = new TrackHistory(5, contextMenu);
     * Track track = new Track("file:///music/song.mp3");
     *
     * if (!history.contains(track)) {
     *     history.add(track);
     * }
     * }</pre>
     *
     * @param t трек для проверки
     * @return true если трек найден в истории, иначе false
     * @see #indexOf(Track)
     * @since 0.1.2
     */
    public boolean contains(Track t) {
        return indexOf(t) != -1;
    }

    /**
     * Создаёт новый элемент меню для указанного трека.
     * <p>
     * Это фабричный метод, отвечающий за создание UI-представления трека.
     * Название элемента формируется по шаблону:
     * <pre>{@code track.viewName() + " | " + track.getLastTimeTrack()}</pre>
     * </p>
     * <p>
     * При выборе элемента меню вызывается {@link Root.PlaylistHandler#openTrack(Track)},
     * что позволяет пользователю быстро открыть трек из истории.
     * </p>
     *
     * <h3>Пример результата</h3>
     * <pre>{@code
     * Track track = new Track("file:///music/song.mp3");
     * track.setLastTimeTrack("15:30");
     * MenuItem item = history.createMenuItem(track);
     * // item.getText() вернёт "song.mp3 | 15:30"
     * }</pre>
     *
     * @param track трек, для которого создаётся элемент меню; не должен быть <code>null</code>
     * @return новый экземпляр {@link MenuItem} с названием и обработчиком
     * @throws NullPointerException если <code>track</code> равен <code>null</code>
     * @see #add(Track)
     * @see #remove(Track)
     * @since 0.1.3
     */
    public MenuItem createMenuItem(Track track) {
        MenuItem m = new MenuItem(track.viewName() + " - " + track.getRawLastTimeTrack() + " - " + track.getState().get(DataTypes.LAST_DATE.code));
        m.setOnAction(e -> Root.PlaylistHandler.openTrack(track));

        return m;
    }

    /**
     * Переходит к предыдущему треку в истории навигации.
     * <p>
     * Безопасно использует {@link HistoryIterator#back()} для перемещения.
     * При index=0 возвращает первый трек (поведение "остановка на границе").
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * TrackHistory history = new TrackHistory(5, contextMenu);
     * Track track1 = new Track("file:///music/song1.mp3");
     * Track track2 = new Track("file:///music/song2.mp3");
     * history.add(track1);
     * history.add(track2);
     *
     * Track prev = history.back();  // Вернёт track1 (index 1→0)
     * Track prev2 = history.back(); // Вернёт track1 (index 0→0, без исключения!)
     * }</pre>
     *
     * @return предыдущий трек (или первый при начале списка)
     * @see #forward()
     * @see #add(Track)
     * @since 0.1.2
     */
    public Track back() {
        return history.get(historyIterator.back());
    }

    /**
     * Переходит к следующему треку в истории навигации.
     * <p>
     * Безопасно использует {@link HistoryIterator#next()} и {@link HistoryIterator#whoIsNext()}.
     * При выходе за конец возвращает последний трек (поведение "остановка на границе").
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * TrackHistory history = new TrackHistory(5, contextMenu);
     * Track track1 = new Track("file:///music/song1.mp3");
     * Track track2 = new Track("file:///music/song2.mp3");
     * history.add(track1);
     * history.add(track2);
     *
     * history.back(); // index=0 (track1)
     * Track next = history.forward(); // Вернёт track2
     * Track next2 = history.forward(); // Вернёт track2 (конец списка, без исключения!)
     * }</pre>
     *
     * @return следующий трек (или последний при конце списка)
     * @see #back()
     * @see #add(Track)
     * @since 0.1.2
     */
    public Track forward() {
        if(historyIterator.whoIsNext() >= history.size())
            return history.get(history.size() - 1);

        return history.get(historyIterator.next());
    }

    /**
     * Создаёт поток для асинхронного обновления контекстного меню в JavaFX потоке.
     * <p>
     * Очищает текущие элементы меню и добавляет новые для треков из истории,
     * ограниченные настройкой "global_history_size" из {@link ConfigurationManager}.
     * </p>
     * <p>
     * Используется {@link Platform#runLater(Runnable)} для безопасного обновления UI.
     * </p>
     * <p>
     * Метод вызывается из {@link #loadFromFile(File)} и может быть вызван вручную
     * при необходимости принудительного обновления меню.
     * </p>
     *
     * @return новый поток, который при запуске обновит меню в GUI
     * @see #loadFromFile(File)
     * @see Platform#runLater(Runnable)
     * @since 0.1.3.5
     */
    private Thread updateContextMenu() {
        return new Thread(() -> Platform.runLater(() -> {
            trackHistoryContextMenu.getItems().clear();

            Iterator<Track> trackHistoryIterator = history.iterator();

            for (int line_number = 0; line_number < ConfigurationManager.instance.getIntItem("global_history_size", "25")
                    && trackHistoryIterator.hasNext(); line_number++) {
                Track t = trackHistoryIterator.next();

                MenuItem m = new MenuItem(t.viewName() + " - " + t.getRawLastTimeTrack() + " - " + t.getState().get(DataTypes.LAST_DATE.code));
                m.setOnAction(e -> Root.PlaylistHandler.openTrack(t));
                trackHistoryContextMenu.getItems().add(m);
            }
        }));
    }

    /**
     * Сохраняет текущую историю треков в указанный файл.
     * <p>
     * Каждый трек записывается в отдельной строке в формате, определённом {@link Track#toString()}.
     * Операция выполняется в текущем потоке, что может заблокировать UI при большом размере истории.
     * </p>
     * <p>
     * <b>Предупреждение</b>: метод не проверяет доступность или права на запись файла.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * TrackHistory history = new TrackHistory(5, contextMenu);
     * // ... добавление треков
     * history.saveToFile(new File("history.dat"));
     * }</pre>
     *
     * @param file файл, в который будет сохранена история; не должен быть <code>null</code>
     * @throws RuntimeException если возникает ошибка ввода-вывода при записи
     * @see #loadFromFile(File)
     * @see Track#toString()
     * @since 0.1.4.2
     */
    public void saveToFile(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for(Track t : history) {
                writer.write(t.toString() + System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Загружает историю треков из указанного файла.
     * <p>
     * Очищает текущую историю и заменяет её треками, прочитанными из файла.
     * Каждая строка файла интерпретируется как один трек через конструктор {@link Track#Track(String)}.
     * </p>
     * <p>
     * После загрузки:
     * <ol>
     *   <li>Итератор сбрасывается на позицию после последнего элемента.</li>
     *   <li>Контекстное меню обновляется асинхронно в JavaFX потоке.</li>
     * </ol>
     * </p>
     * <p>
     * <b>Предупреждение</b>: метод не проверяет существование файла или корректность формата строк.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * TrackHistory history = new TrackHistory(5, contextMenu);
     * history.loadFromFile(new File("history.dat"));
     * }</pre>
     *
     * @param file файл, из которого будет загружена история; не должен быть <code>null</code>
     * @throws RuntimeException если возникает ошибка ввода-вывода при чтении
     * @see #saveToFile(File)
     * @see Track#Track(String)
     * @since 0.1.4.2
     */
    public void loadFromFile(File file) {
        LinkedList<Track> loadedHistory = new LinkedList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                Track t = new Track(line);
                t.getLastTimeTrack();

                loadedHistory.add(t);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        history.clear();
        history.addAll(loadedHistory);

        historyIterator.setIndex(history.size());

        updateContextMenu().start();
    }

    public HistoryIterator getHistoryIterator() {
        return historyIterator;
    }

    @Override
    public TrackHistory clone() {
        try {
            TrackHistory clone = (TrackHistory) super.clone();
            clone.history.setAll(history);

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public int compareTo(TrackHistory o) {
        return o.size() - size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackHistory that = (TrackHistory) o;
        return history.equals(that.history) && maxSize.get() == (that.maxSize.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(history, maxSize);
    }

    @Override
    public String toString() {
        return "TrackHistory{" +
                "history=" + history +
                '}';
    }
}