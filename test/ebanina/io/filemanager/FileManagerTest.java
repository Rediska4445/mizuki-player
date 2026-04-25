package ebanina.io.filemanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rf.ebanina.File.FileManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileManagerTest {
    private final FileManager fileManager = new FileManager("");

    @TempDir
    Path tempDir;

    @Test
    void createCacheDirectoryIfNotExist_createsFilesOnlyIfTheyDoNotExist(@TempDir Path tempDir) throws IOException {
        Path baseDir = tempDir.resolve("cache");
        Path cache   = baseDir.resolve("cache");
        Path inet    = baseDir.resolve("inet");

        Files.deleteIfExists(tempDir.resolve("cache"));

        FileManager.createCacheDirectoryIfNotExist(baseDir);

        // Проверяем, что все директории и файлы существуют
        System.out.println(baseDir);

        assertTrue(Files.exists(baseDir));
        assertTrue(Files.isDirectory(baseDir));
        assertTrue(Files.exists(cache));
        assertTrue(Files.isDirectory(cache));
        assertTrue(Files.exists(cache.resolve("playlists")));
        assertTrue(Files.isDirectory(cache.resolve("playlists")));
        assertTrue(Files.exists(cache.resolve("tags")));
        assertTrue(Files.isDirectory(cache.resolve("tags")));
        assertTrue(Files.exists(cache.resolve("tracks")));
        assertTrue(Files.isDirectory(cache.resolve("tracks")));
        assertTrue(Files.exists(inet));
        assertTrue(Files.isDirectory(inet));

        Path historyPath = cache.resolve("history.txt");
        Path sharedPath  = baseDir.resolve("shared.txt");

        assertTrue(Files.exists(historyPath));
        assertTrue(Files.exists(sharedPath));
    }

    @Test
    void createCacheDirectoryIfNotExist_doesNotOverwriteExistingFiles(@TempDir Path tempDir) throws IOException {
        Path baseDir = tempDir.resolve("cache");
        Path cache   = baseDir.resolve("cache");
        Path historyPath = cache.resolve("history.txt");
        Path sharedPath  = baseDir.resolve("shared.txt");

        // создадим уже существующие файлы с другим содержимым
        Files.createDirectories(baseDir);
        Files.createDirectories(cache);
        Files.writeString(historyPath, "already exists history", StandardCharsets.UTF_8);
        Files.writeString(sharedPath, "already exists shared", StandardCharsets.UTF_8);

        FileManager.createCacheDirectoryIfNotExist(baseDir);

        // проверяем, что файлы не перезаписались (содержимое осталось прежнее)
        String historyContent = Files.readString(historyPath, StandardCharsets.UTF_8);
        String sharedContent  = Files.readString(sharedPath, StandardCharsets.UTF_8);

        assertEquals("already exists history", historyContent);
        assertEquals("already exists shared", sharedContent);
    }

    @Test
    void createCacheDirectoryIfNotExist_createsMissingFilesOnly(@TempDir Path tempDir) throws IOException {
        Path baseDir = tempDir.resolve("cache");
        Path cache   = baseDir.resolve("cache");
        Path inet    = baseDir.resolve("inet");

        // изначально есть только cache/cache, остальное отсутствует
        Files.createDirectories(baseDir);
        Files.createDirectories(cache); // cache/cache существует

        // history.txt и shared.txt — нет
        Path historyPath = cache.resolve("history.txt");
        Path sharedPath  = baseDir.resolve("shared.txt");

        assertFalse(Files.exists(historyPath));
        assertFalse(Files.exists(sharedPath));

        // playlist, tags, tracks — тоже нет
        assertFalse(Files.exists(cache.resolve("playlists")));
        assertFalse(Files.exists(cache.resolve("tags")));
        assertFalse(Files.exists(cache.resolve("tracks")));
        assertFalse(Files.exists(inet));

        FileManager.createCacheDirectoryIfNotExist(baseDir);

        // проверяем, что:
        // - cache/cache не пересоздаётся (но это не важно, Files.createDirectories не ломает)
        // - недостающие директории и файлы появились
        assertTrue(Files.exists(cache.resolve("playlists")));
        assertTrue(Files.exists(cache.resolve("tags")));
        assertTrue(Files.exists(cache.resolve("tracks")));
        assertTrue(Files.exists(inet));
        assertTrue(Files.exists(historyPath));
        assertTrue(Files.exists(sharedPath));
    }

    /**
     * Тест: успешное сохранение коллекции.
     */
    @Test
    public void testSaveCollection_success() throws IOException {
        String path = tempDir.resolve("test_collection.txt").toString();
        String type = "properties";
        String track = "main";

        Map<String, String> data = Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3"
        );

        fileManager.saveCollection(path, type, track, data);

        assertTrue(Files.exists(Path.of(path)), "Файл должен быть создан");

        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("key1=value1"), "Файл должен содержать key1=value1");
        assertTrue(content.contains("key2=value2"), "Файл должен содержать key2=value2");
        assertTrue(content.contains("key3=value3"), "Файл должен содержать key3=value3");
    }

    /**
     * Тест: чтение существующей коллекции.
     */
    @Test
    public void testReadCollection_success() throws IOException {
        String path = tempDir.resolve("read_test.txt").toString();
        String type = "properties";
        String track = "main";

        Map<String, String> expected = new HashMap<>();
        expected.put("alpha", "100");
        expected.put("beta", "200");
        expected.put("gamma", "300");

        fileManager.saveCollection(path, type, track, expected);

        Map<String, String> actual = fileManager.readCollection(path, type, track, Collections.emptyMap());

        assertEquals(expected, actual, "Считанная коллекция должна совпадать с сохранённой");
    }

    /**
     * Тест: чтение несуществующего файла → возвращается ifEmpty.
     */
    @Test
    public void testReadCollection_fileNotExists_returnsIfEmpty() {
        String path = tempDir.resolve("nonexistent.txt").toString();
        String type = "properties";
        String track = "main";

        Map<String, String> ifEmpty = Map.of(
                "defaultKey", "defaultValue",
                "anotherKey", "anotherValue"
        );

        Map<String, String> result = fileManager.readCollection(path, type, track, ifEmpty);

        assertEquals(ifEmpty, result, "При отсутствии файла должен вернуться ifEmpty");
    }

    /**
     * Тест: чтение пустого файла → возвращается ifEmpty.
     */
    @Test
    public void testReadCollection_emptyFile_returnsIfEmpty() throws IOException {
        String path = tempDir.resolve("empty.txt").toString();
        String type = "properties";
        String track = "main";

        Files.writeString(Path.of(path), "");

        Map<String, String> ifEmpty = Map.of("fallback", "value");

        Map<String, String> result = fileManager.readCollection(path, type, track, ifEmpty);

        assertEquals(ifEmpty, result, "При пустом файле должен вернуться ifEmpty");
    }

    /**
     * Тест: сохранение с пустой Map данных.
     */
    @Test
    public void testSaveCollection_emptyMap() throws IOException {
        String path = tempDir.resolve("empty_collection.txt").toString();
        String type = "properties";
        String track = "main";

        fileManager.saveCollection(path, type, track, Collections.emptyMap());

        assertTrue(Files.exists(Path.of(path)), "Файл должен быть создан даже для пустой Map");
    }

    /**
     * Тест: круговой сценарий — сохранение разных типов и track'ов.
     */
    @Test
    public void testSaveAndReadCollection_differentTypesAndTracks() throws IOException {
        String base = tempDir.resolve("multi").toString();

        Map<String, String> dataSettings = Map.of("theme", "dark", "lang", "ru");
        Map<String, String> dataHistory = Map.of("item1", "2025-01-01", "item2", "2025-06-15");

        fileManager.saveCollection(base + "_settings.txt", "settings", "user", dataSettings);
        fileManager.saveCollection(base + "_history.txt", "history", "audit", dataHistory);

        Map<String, String> settings = fileManager.readCollection(
                base + "_settings.txt", "settings", "user", Collections.emptyMap());
        Map<String, String> history = fileManager.readCollection(
                base + "_history.txt", "history", "audit", Collections.emptyMap());

        assertEquals(dataSettings, settings);
        assertEquals(dataHistory, history);
    }

    /**
     * Тест: сохранение с null-значением (если ваша реализация это допускает).
     * Если бросается NullPointerException — уберите assertDoesNotThrow и ожидайте исключение.
     */
    @Test
    public void testSaveCollection_withNullValues() {
        String path = tempDir.resolve("null_test.txt").toString();
        String type = "properties";
        String track = "main";

        Map<String, String> data = new HashMap<>();
        data.put("key", null);

        assertDoesNotThrow(() -> fileManager.saveCollection(path, type, track, data),
                "Сохранение с null-значением не должно бросать исключение (если это поддерживается)");
    }
    private static final DateTimeFormatter MINUTE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Тест: сохранение коллекции, где первый элемент — текущее время (до минуты).
     * Предполагается, что в Map есть ключ "timestamp" (или аналогичный),
     * который содержит строку в формате "yyyy-MM-dd HH:mm".
     */
    @Test
    public void testSaveCollection_withCurrentTimestamp() throws IOException {
        String path = tempDir.resolve("timestamp_collection.txt").toString();
        String type = "events";
        String track = "log";

        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        String timestampStr = now.format(MINUTE_FORMATTER);

        Map<String, String> data = new HashMap<>();
        data.put("timestamp", timestampStr);       // первый/ключевой элемент — время
        data.put("event", "test_event");
        data.put("status", "created");

        fileManager.saveCollection(path, type, track, data);

        assertTrue(Files.exists(Path.of(path)), "Файл с временной меткой должен быть создан");

        String content = Files.readString(Path.of(path));
        assertTrue(content.contains("timestamp=" + timestampStr),
                "Файл должен содержать временную метку в формате yyyy-MM-dd HH:mm");
        assertTrue(content.contains("event=test_event"), "Файл должен содержать событие");
    }

    /**
     * Тест: чтение коллекции за определённный период.
     * Создаём несколько записей с разными timestamps, затем читаем
     * и проверяем, что запись с датой внутри периода попадает в результат.
     *
     * Примечание: если ваш parseCollection не умеет фильтровать по периоду,
     * этот тест проверяет, что все записи (включая датированные) корректно парсятся,
     * а фильтрацию по времени вы будете делать на уровне вызывающего кода.
     */
    @Test
    public void testReadCollection_forSpecificPeriod() throws IOException {
        String path = tempDir.resolve("period_collection.txt").toString();
        String type = "events";
        String track = "audit";

        // Формируем несколько записей с разными timestamp'ами
        LocalDateTime start = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        LocalDateTime mid = start.plusMinutes(10);
        LocalDateTime end = start.plusMinutes(20);

        Map<String, String> data1 = new HashMap<>();
        data1.put("timestamp", start.format(MINUTE_FORMATTER));
        data1.put("event", "start_event");

        Map<String, String> data2 = new HashMap<>();
        data2.put("timestamp", mid.format(MINUTE_FORMATTER));
        data2.put("event", "mid_event");

        Map<String, String> data3 = new HashMap<>();
        data3.put("timestamp", end.format(MINUTE_FORMATTER));
        data3.put("event", "end_event");

        // Сохраняем все три записи (как одну коллекцию, где каждый элемент — отдельная строка)
        // Если ваша реализация saveCollection принимает только одну Map,
        // то здесь мы сохраняем три разных файла с разными track'ами или разными именами.
        // В примере ниже — три отдельных файла с разными track'ами для простоты:
        fileManager.saveCollection(
                tempDir.resolve("period_1.txt").toString(), type, "track_1", data1);
        fileManager.saveCollection(
                tempDir.resolve("period_2.txt").toString(), type, "track_2", data2);
        fileManager.saveCollection(
                tempDir.resolve("period_3.txt").toString(), type, "track_3", data3);

        // Читаем каждую коллекцию
        Map<String, String> result1 = fileManager.readCollection(
                tempDir.resolve("period_1.txt").toString(), type, "track_1", Collections.emptyMap());
        Map<String, String> result2 = fileManager.readCollection(
                tempDir.resolve("period_2.txt").toString(), type, "track_2", Collections.emptyMap());
        Map<String, String> result3 = fileManager.readCollection(
                tempDir.resolve("period_3.txt").toString(), type, "track_3", Collections.emptyMap());

        // Проверяем, что timestamp корректно сохранён и прочитан
        assertEquals(start.format(MINUTE_FORMATTER), result1.get("timestamp"),
                "Первая запись должна иметь timestamp в начале периода");
        assertEquals(mid.format(MINUTE_FORMATTER), result2.get("timestamp"),
                "Вторая запись должна иметь timestamp в середине периода");
        assertEquals(end.format(MINUTE_FORMATTER), result3.get("timestamp"),
                "Третья запись должна иметь timestamp в конце периода");

        // Теперь имитируем фильтрацию по периоду (например, от start до mid включительно)
        LocalDateTime periodStart = start;
        LocalDateTime periodEnd = mid;

        Map<String, String> filtered = new HashMap<>();
        for (Map<String, String> r : java.util.List.of(result1, result2, result3)) {
            String ts = r.get("timestamp");
            if (ts == null) continue;
            LocalDateTime tsTime = LocalDateTime.parse(ts, MINUTE_FORMATTER);
            if (!tsTime.isBefore(periodStart) && !tsTime.isAfter(periodEnd)) {
                filtered.putAll(r);
            }
        }

        // В фильтре должны быть запись1 и запись2, но не запись3
        assertTrue(filtered.containsKey("timestamp"),
                "Отфильтрованная коллекция должна содержать timestamp");
        // Проверяем, что mid_event есть (поскольку mid внутри периода)
        String eventInFiltered = filtered.get("event");
        assertTrue(
                "start_event".equals(eventInFiltered) || "mid_event".equals(eventInFiltered),
                "В периоде должны быть start_event или mid_event");
    }
}