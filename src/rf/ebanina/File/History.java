package rf.ebanina.File;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class History<R extends Reference>
        implements Serializable, Comparable<History<R>>
{
    @Serial
    private static final long serialVersionUID = 3L;

    protected final ObservableList<R> history = FXCollections.observableArrayList();

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
                writer.write(t.getPath() + System.lineSeparator());
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
    public int compareTo(@NotNull History<R> o) {
        return 0;
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
