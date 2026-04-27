package rf.ebanina.File;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Универсальная реализация истории переходов (аналог вкладки "История" в веб-браузере)
 * для объектов типа {@link Referencable}.
 * <p>
 * Предназначен для отслеживания последовательности посещенных/открытых файловых ресурсов,
 * медиафайлов, документов или других ссылок с поддержкой навигации "назад/вперед",
 * автоматическим ограничением объема памяти и персистентностью через файловую систему.
 * </p>
 *
 * <h3>Ключевые особенности:</h3>
 * <ul>
 *   <li><strong>Автоматическое управление размером:</strong> поддержка максимального количества
 *       элементов с автоматическим удалением самых старых при переполнении (FIFO)</li>
 *   <li><strong>Навигационный итератор:</strong> встроенный {@link HistoryIterator} для
 *       перемещения по истории с методами {@code back()}, {@code forward()}</li>
 *   <li><strong>JavaFX интеграция:</strong> {@link ObservableList} для реактивного UI</li>
 *   <li><strong>Персистентность:</strong> сохранение/загрузка в текстовые файлы с использованием
 *       {@link ReferenceFactory}</li>
 *   <li><strong>Сравнение историй:</strong> лексикографическое сравнение по путям элементов</li>
 *   <li><strong>Потокобезопасный итератор:</strong> использование {@link AtomicInteger}</li>
 * </ul>
 *
 * <h3>Типичное применение:</h3>
 * <pre>{@code
 * // Создание истории на 50 элементов
 * History<TrackReference> musicHistory = new History<>(50);
 *
 * // Добавление трека (автоматически ограничивает размер)
 * musicHistory.add(currentTrack);
 *
 * // Навигация
 * TrackReference previous = musicHistory.back();
 * TrackReference next = musicHistory.forward();
 *
 * // Сохранение в файл
 * musicHistory.saveToFile(new File("music_history.txt"));
 *
 * // Привязка к ListView в JavaFX
 * listView.setItems(musicHistory.getHistory());
 * }</pre>
 *
 * <h3>Жизненный цикл:</h3>
 * <ol>
 *   <li>Инициализация с максимальным размером</li>
 *   <li>Добавление элементов через {@code add()} - автоматическое удаление старых при переполнении</li>
 *   <li>Навигация через итератор (позиция автоматически обновляется при добавлении)</li>
 *   <li>Сохранение/загрузка состояния</li>
 * </ol>
 *
 * <h3>Ограничения:</h3>
 * <ul>
 *   <li>Удаление элементов вручную сбрасывает итератор в конец</li>
 *   <li>Сохранение работает только с путями ({@link Referencable#path()} ()})</li>
 *   <li>Для десериализации требуется фабрика {@link ReferenceFactory}</li>
 * </ul>
 *
 * <p><strong>Сериализуемость:</strong> полная поддержка {@link Serializable} для сохранения
 * состояния в бинарном виде.</p>
 *
 * @param <R> тип элементов истории, должен наследовать {@link Referencable} и быть сериализуемым
 * @implements Serializable для персистентности состояния
 * @implements Comparable&lt;History&lt;R&gt;&gt; для сортировки коллекций историй
 *
 * @see History.HistoryIterator внутренний итератор навигации
 * @see Referencable базовый интерфейс элементов истории
 * @see ReferenceFactory фабрика для восстановления объектов из строк
 */
public class History<R extends Referencable>
        implements Serializable, Comparable<History<R>>
{
    /**
     * Идентификатор версии для корректной сериализации/десериализации класса.
     * <p>
     * Значение <code>3L</code> соответствует текущей структуре класса. При изменении
     * полей или логики сериализации рекомендуется обновить значение для предотвращения
     * ошибок десериализации старых объектов.
     *
     * @serial поле используется JVM для проверки совместимости версий
     * @see Serializable
     */
    @Serial
    private static final long serialVersionUID = 3L;

    /**
     * Основное хранилище элементов истории.
     * <p>
     * ObservableList обеспечивает:
     * <ul>
     *   <li>Автоматическое уведомление UI о изменениях (JavaFX binding)</li>
     *   <li>Поддержку итерации и поиска</li>
     *   <li>Реактивное обновление ListView/TableView при добавлении/удалении</li>
     * </ul>
     * Элементы хранятся в порядке добавления (самые новые - в конце).
     *
     * @see FXCollections#observableArrayList()
     */
    protected final ObservableList<R> history = FXCollections.observableArrayList();

    /**
     * Максимально допустимое количество элементов в истории.
     * <p>
     * JavaFX Property для динамической привязки к UI элементам (Slider, TextField).
     * При превышении лимита автоматически удаляется первый (самый старый) элемент.
     *
     * <p><strong>Пример привязки:</strong></p>
     * <pre>{@code
     * Slider sizeSlider = new Slider(10, 1000, 100);
     * sizeSlider.valueProperty().bindBidirectional(history.maxSizeProperty());
     * }</pre>
     *
     * @see IntegerProperty
     * @see SimpleIntegerProperty
     */
    protected final IntegerProperty maxSize = new SimpleIntegerProperty();

    protected HistoryIterator historyIterator;

    public History(int maxSize) {
        this.maxSize.set(maxSize);
        this.historyIterator = new HistoryIterator();
    }

    public void remove(R track) {
        history.remove(track);

        historyIterator.setIndex(history.size());
    }

    public void add(R track) {
        if(history.size() >= maxSize.get()) {
            history.remove(0);
        }

        history.add(track);

        historyIterator.setIndex(history.size() - 1);
    }

    public int indexOf(R t) {
        return history.indexOf(t);
    }

    public int size() {
        return history.size();
    }

    public boolean contains(R t) {
        return indexOf(t) != -1;
    }

    public R back() {
        return history.get(historyIterator.back());
    }

    public R forward() {
        if(historyIterator.whoIsNext() >= history.size())
            return history.get(history.size() - 1);

        return history.get(historyIterator.next());
    }

    public void saveToFile(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for(R t : history) {
                writer.write(t.path() + System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadFromFile(File file, ReferenceFactory<R> factory) {
        LinkedList<R> loaded = new LinkedList<>();

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = r.readLine()) != null) {
                R item = factory.fromString(line);
                loaded.add(item);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load history", e);
        }

        history.clear();
        history.addAll(loaded);
        historyIterator.setIndex(history.size());
    }

    public ObservableList<R> getHistory() {
        return history;
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

    public HistoryIterator getHistoryIterator() {
        return historyIterator;
    }

    public History<R> setHistoryIterator(HistoryIterator historyIterator) {
        this.historyIterator = historyIterator;
        return this;
    }

    @Override
    public int compareTo(History<R> o) {
        int len1 = this.history.size();
        int len2 = o.history.size();
        int limit = Math.min(len1, len2);

        for (int i = 0; i < limit; i++) {
            int cmp = (this.history.get(i)).path().compareTo(o.history.get(i).path());
            if (cmp != 0)
                return cmp;
        }

        return Integer.compare(len1, len2);
    }

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
}
