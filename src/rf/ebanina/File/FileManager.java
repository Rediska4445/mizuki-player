package rf.ebanina.File;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.json.simple.parser.ParseException;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.MediaPlayer;
import rf.ebanina.ebanina.Player.Playlist;
import rf.ebanina.ebanina.Player.Track;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static rf.ebanina.ebanina.Music.*;

public class FileManager
        implements rf.ebanina.File.IDataFiles, IFileProcessor
{
    public static FileManager instance = new FileManager(Resources.Properties.DEFAULT_COMMON_CACHE_PATH.getKey());

    public static final String DEFAULT_USER_MUSIC_PATH = "Music";
    public static final String DEFAULT_USER_MUSIC_KEY = "default_User_music";
    private final String sharedPath;

    public FileManager(String sharedPath) {
        this.sharedPath = sharedPath;
    }

    public static void createCacheDirectoryIfNotExist() {
        String cacheDir = "cache";

        try {
            final Path of = Path.of(cacheDir);

            if(!Files.exists(of)) {
                Files.createDirectories(of);

                final Path cache = Path.of(of.toString(), "cache");

                if(!Files.exists(cache)) {
                    Files.createDirectories(cache);

                    final Path playlists = Path.of(cache.toString(), "playlists");

                    if(!Files.exists(playlists)) {
                        Files.createDirectories(playlists);
                    }

                    final Path tags = Path.of(cache.toString(), "tags");

                    if(!Files.exists(tags)) {
                        Files.createDirectories(tags);
                    }

                    final Path tracks = Path.of(cache.toString(), "tracks");

                    if(!Files.exists(tracks)) {
                        Files.createDirectories(tracks);
                    }

                    Path historyPath = cache.resolve("history.txt");
                    if (!Files.exists(historyPath)) {
                        Files.writeString(historyPath, "", StandardOpenOption.CREATE_NEW);
                    }
                }

                final Path inet = Path.of(of.toString(), "inet");

                if(!Files.exists(inet)) {
                    Files.createDirectories(inet);
                }

                Path sharedPath = of.resolve("shared.txt");
                if (!Files.exists(sharedPath)) {
                    String content = """
                        array of (app) = [license_agreed=false, full_time=0];
                        array of (main_window) = [right_list_open=true, left_list_open=true, width=1132.0, layout_x=68.0, layout_y=304.0, height=711.0];
                        array of (mediaPlayer) = [volume=1.0, tempo=1.0, pitch=1.0, pan=0.05, pause=false];
                        array of (player) = [last_track_index_playlist_local=0, last_track_index_local=0, last_track_time=0.0]
                        """;
                    Files.writeString(sharedPath, content, StandardOpenOption.CREATE_NEW);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ArrayList<Track> getMusic(Path path) throws IOException {
        ArrayList<Track> t = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(path, FOLLOW_LINKS)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(this::hasSupportedExtension)
                    .forEach(file -> {
                        Track track = new Track(file.toAbsolutePath().toString());
                        t.add(track);
                    });
        }

        return t;
    }

    @Override
    public File getFileFromOpenFileDialog(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(name);

        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory == null) {
            selectedDirectory = new File(java.lang.System.getProperty("user.home") + File.separator + DEFAULT_USER_MUSIC_PATH);
        }

        return selectedDirectory;
    }

    @Override
    public void setPlaylists(String pathMainFolder) throws IOException {
        PlayProcessor.playProcessor.getCurrentPlaylist().clear();

        ArrayList<Playlist> playlistFiles = getPlaylists(pathMainFolder);

        List<Playlist> playlistStrings = new ArrayList<>(playlistFiles);

        PlayProcessor.playProcessor.getCurrentPlaylist().addAll(playlistStrings);
    }

    @Override
    public ArrayList<Playlist> getPlaylists(String pathMainFolder) throws IOException {
        ArrayList<Playlist> files = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(Paths.get(pathMainFolder), FOLLOW_LINKS)) {
            stream
                    .filter(Files::isDirectory)
                    .filter(dir -> !dir.toAbsolutePath().toString().equals(PlayProcessor.playProcessor.getCurrentDefaultMusicDir()))
                    .forEach(dir -> files.add(new Playlist(dir.toFile().getAbsolutePath())));
        }

        return files;
    }

    @Override
    public List<String> supportedExtensions() {
        return List.of(
                MediaPlayer.AvailableFormat.MP3.getTitle(), MediaPlayer.AvailableFormat.WAV.getTitle()
        );
    }

    @Override
    public boolean hasSupportedExtension(Path path) {
        return supportedExtensions().stream().anyMatch(ext -> path.getFileName().toString().toLowerCase().endsWith(ext));
    }

    public final boolean isOccupiedSpace(String path, long space) {
        File file = new File(path);
        if (!file.exists()) {
            return false;
        }

        long totalSpace = file.getTotalSpace();
        long usableSpace = file.getUsableSpace();
        long usedSpace = totalSpace - usableSpace;

        long thresholdBytes = space * 1024 * 1024;

        return usedSpace >= thresholdBytes;
    }

    public Path createDirectoryIfNotExists(String dirPath) {
        File directory = new File(dirPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        return directory.toPath();
    }

    public void clearCacheData(String path, java.util.function.Predicate<Path> predicate) {
        try (Stream<Path> files = Files.walk(createDirectoryIfNotExists(path))) {
            files.filter(predicate).forEach((e1) -> (e1).toFile().delete());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearCacheData(String path) {
        clearCacheData(path, e1 -> hasSupportedExtension(Path.of(path)));
    }

    private final Map<String, String> loadedSharedMap = new HashMap<>();

    public String getVersion() {
        try {
            return rf.ebanina.utils.formats.json.JsonProcess.getJsonItem(rf.ebanina.utils.formats.json.JsonProcess.getJsonItem(String.join("", FileManager.instance.findParams(
                    null, Path.of("version.json"), List::of, s -> true
            )), "version"), "serial");
        } catch (ParseException e) {
            e.printStackTrace();

            return "0";
        }
    }

    public record SharedDataEntry(
            String category,
            String key,
            Supplier<String> valueSupplier,
            String defaultValue
    ) {}

    public List<SharedDataEntry> saveSharedData = new ArrayList<>(List.of(
            new SharedDataEntry("mediaPlayer", "tempo", () -> String.valueOf(MediaProcessor.mediaProcessor.globalMap.get("tempo", float.class)), "1.0"),
            new SharedDataEntry("mediaPlayer", "volume", () -> String.valueOf(MediaProcessor.mediaProcessor.globalMap.get("volume", double.class)), "1.0"),
            new SharedDataEntry("mediaPlayer", "pause", () -> String.valueOf(MediaPlayer.Status.PAUSED == MediaProcessor.mediaProcessor.mediaPlayer.getStatus()), "true"),
            new SharedDataEntry("mediaPlayer", "pan", () -> String.valueOf(MediaProcessor.mediaProcessor.globalMap.get("pan", float.class)), "0.0"),
            new SharedDataEntry("player", "last_track_time", () -> String.valueOf(MediaProcessor.mediaProcessor.mediaPlayer.getCurrentTime().toSeconds()), "0"),
            new SharedDataEntry("player", "last_track_index_local", () -> String.valueOf(PlayProcessor.playProcessor.getTrackIter()), "0"),
            new SharedDataEntry("player", "last_track_index_playlist_local", () -> String.valueOf(PlayProcessor.playProcessor.getCurrentPlaylistIter()), "0"),
            new SharedDataEntry("history", "tracks", () -> PlayProcessor.playProcessor.getTrackHistoryGlobal().getHistory().toString(), "[]"),
            new SharedDataEntry("main_window", "width", () -> String.valueOf(Root.rootImpl.stage.getWidth()), "800"),
            new SharedDataEntry("main_window", "height", () -> String.valueOf(Root.rootImpl.stage.getHeight()), "600"),
            new SharedDataEntry("main_window", "layout_x", () -> String.valueOf(Root.rootImpl.stage.getX()), "0"),
            new SharedDataEntry("main_window", "layout_y", () -> String.valueOf(Root.rootImpl.stage.getY()), "0"),
            new SharedDataEntry("main_window", "left_list_open", () -> String.valueOf(Root.rootImpl.similar.isOpened()), "false"),
            new SharedDataEntry("main_window", "right_list_open", () -> String.valueOf(Root.rootImpl.tracksListView.isOpened()), "false"),
            new SharedDataEntry("app", "full_time", () -> {
                if(!readSharedData().containsKey("full_time")) {
                    return "-100";
                } else {
                    return String.valueOf(
                            ((Float.parseFloat(readSharedData().getOrDefault("full_time", "-100")) * 1000)
                                    + (System.currentTimeMillis() - startTimeMs.get())) * 0.001
                    );
                }
            }, "-100")
    ));

    public void saveSharedData() {
        Map<String, Map<String, String>> groupedData = new HashMap<>();

        for (SharedDataEntry entry : saveSharedData) {
            String category = entry.category();
            String key = entry.key();
            String value = entry.valueSupplier().get();

            groupedData.computeIfAbsent(category, k -> new HashMap<>()).put(key, value);
        }

        for (Map.Entry<String, Map<String, String>> groupEntry : groupedData.entrySet()) {
            String category = groupEntry.getKey();
            Map<String, String> params = groupEntry.getValue();
            saveArray(sharedPath, category, params);
        }
    }

    public Map<String, String> readSharedData() {
        if (loadedSharedMap.isEmpty()) {
            Map<String, String> flatMap = new HashMap<>();
            Set<String> categories = saveSharedData.stream()
                    .map(SharedDataEntry::category)
                    .collect(Collectors.toSet());

            for (String category : categories) {
                Map<String, String> defaultMap = saveSharedData.stream()
                        .filter(e -> e.category().equals(category))
                        .collect(Collectors.toMap(
                                SharedDataEntry::key,
                                SharedDataEntry::defaultValue
                        ));

                Map<String, String> map = readArray(sharedPath, category, defaultMap);
                flatMap.putAll(map);
            }

            loadedSharedMap.putAll(flatMap);
        }

        return loadedSharedMap;
    }

    public void loadSession(Map<String, String> in) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("session.ini"))) {
            for (Map.Entry<String, String> entry : in.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSession() {
        FileManager.instance.loadSession(new HashMap<>(Map.ofEntries(
                Map.entry("user.name", System.getProperty("user.name")),
                Map.entry("application.name", appName),
                Map.entry("version", version),
                Map.entry("starting.time", LocalDateTime.now().toString())
        )));
    }

    public void writeObject(Object obj, String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filePath))) {
            oos.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object readObject(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filePath))) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public final String splitData(String data) {
        String trim = data;

        if (data.contains("=") || data.endsWith(";")) {
            String trim1 = data.replace(" ", "");
            trim = trim1.substring(trim1.indexOf("=") + 1, trim1.lastIndexOf(";"));
        }

        return trim;
    }

    public String getDefaultMusicPath() {
        String home = System.getProperty("user.home");

        if (System.getProperty("os.name").contains("win")) {
            String userProfile = System.getenv("USERPROFILE");

            if (userProfile != null) {
                return userProfile + File.separator + DEFAULT_USER_MUSIC_PATH;
            } else {
                return home + File.separator + DEFAULT_USER_MUSIC_PATH;
            }
        } else {
            return home + File.separator + DEFAULT_USER_MUSIC_PATH;
        }
    }

    public final String splitDataWithSpaces(String data, String begin) {
        if(!data.endsWith(";"))
            return data;

        return data.substring(data.indexOf(begin) + 1, data.indexOf(";"));
    }

    public BiFunction<String, String, Boolean> stdProcessor = String::startsWith;

    public final String findFirstParam(Path path, String ifNull) {
        return findParams(null, path, ifNull).get(0);
    }

    public final String findFirstParam(String key, Path path, String ifNull) {
        List<String> a = findParams(key, path, ifNull);

        if(a == null || a.size() == 0) {
            return ifNull;
        }

        return a.get(0);
    }

    public final String findFirstParam(String key, Path path, Exception ifNull) {
        List<String> a = findParams(key, path, ifNull);

        if(a == null || a.size() == 0) {
            throw new RuntimeException(ifNull);
        }

        return a.get(0);
    }

    public final String findFirstParam(Path path, String ifNull, Predicate<String> processor) {
        List<String> a = findParams(path, ifNull, processor);

        if(a == null || a.size() == 0) {
            return ifNull;
        }

        return a.get(0);
    }

    public final List<String> findParams(Path path, String ifNull, Predicate<String> processor) {
        return findParams("", path, () -> List.of(ifNull), processor);
    }

    public final List<String> findParams(String key, Path path, String ifNull) {
        return findParams(key, path, () -> List.of(ifNull), s -> stdProcessor.apply(s, key));
    }

    public final List<String> findParams(String key, Path path, Exception ifNull) {
        return findParams(key, path, () -> {
            throw new RuntimeException(ifNull);
        }, s -> stdProcessor.apply(s, key));
    }

    /**
     * Читает и фильтрует строки из файла по заданному пути.
     *
     * <p>Метод выполняет следующие шаги:</p>
     * <ol>
     *   <li>Проверяет существование файла по пути {@code path}.
     *       Если файл отсутствует — возвращает результат {@code ifNull.get()}.</li>
     *   <li>Открывает поток строк из файла с помощью {@link Files#lines(Path)}
     *       в блоке try-with-resources (автоматическое закрытие).</li>
     *   <li>Если {@code key == null} — возвращает ВСЕ строки файла
     *       ({@code lines.toList()}).</li>
     *   <li>Если {@code key != null} — применяет фильтрацию через
     *       {@code processor} ко всем строкам и возвращает отфильтрованный список.</li>
     *   <li>При ЛЮБОМ исключении ({@link Exception}, включая {@link IOException})
     *       возвращает результат {@code ifNull.get()}.</li>
     * </ol>
     *
     * <p>Метод <b>НЕ выбрасывает исключения</b> — все ошибки обрабатываются
     * возвратом дефолтного значения из {@code ifNull}.</p>
     *
     * <h3>Точное поведение параметров:</h3>
     * <ul>
     *   <li>{@code key == null} → игнорируется {@code processor},
     *      возвращаются все строки файла</li>
     *   <li>{@code key != null} → применяется {@code processor} к каждой строке</li>
     *   <li>{@code processor} вызывается ТОЛЬКО при {@code key != null}</li>
     *   <li>{@code ifNull.get()} вызывается при:
     *     <ul>
     *       <li>!{@link Files#exists(Path, LinkOption...)}}</li>
     *       <li>любом {@link Exception} при чтении/фильтрации</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h3>Примеры ЛОГИКИ (строго по коду):</h3>
     * <pre>{@code
     * // key == null → все строки
     * findParams(null, path, ifNull, ignoredProcessor); // lines.toList()
     *
     * // файл НЕ существует → ifNull
     * findParams(key, nonExistentPath, ifNull, processor); // ifNull.get()
     *
     * // IOException → ifNull
     * findParams(key, corruptedPath, ifNull, processor); // ifNull.get()
     *
     * // key != null + processor.filter() → отфильтрованные строки
     * findParams("abc", validPath, ifNull, line -> line.contains("abc"));
     * }</pre>
     *
     * @param key если {@code null} — возвращает все строки, иначе фильтрует через processor
     * @param path путь к файлу для чтения строк
     * @param ifNull вызывается при отсутствии файла ИЛИ любой ошибке чтения
     * @param processor применяется к строкам ТОЛЬКО при {@code key != null}
     * @return список строк из файла или результат {@code ifNull.get()}
     */
    public List<String> findParams(String key, Path path, Supplier<List<String>> ifNull, Predicate<String> processor) {
        if (!Files.exists(path)) {
            return ifNull.get();
        }

        try (Stream<String> lines = Files.lines(path)) {
            if(key == null) {
                return lines.toList();
            }

            return lines.filter(processor).toList();
        } catch (Exception e) {
            return ifNull.get();
        }
    }

    private final Object fileLock = new Object();

    private void isExist(String path) throws IOException {
        final java.nio.file.Path path1 = Paths.get(ResourceManager.Instance.getFullyPath(path));

        if(!Files.exists(path1)) {
            Files.createFile(path1);
        }
    }

    private int getLines(String path, Predicate<String> predicate) {
        int lines = 0;
        String line;

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            while ((line = reader.readLine()) != null) {
                if (predicate.test(line)) {
                    lines++;
                }
            }
        } catch (IOException e) {
            Music.mainLogger.err(e);
        }

        return lines;
    }

    public Map<String, String> parseArray(String arrayString, Map<String, String> ifEmpty) {
        Map<String, String> map = new HashMap<>();

        if (arrayString == null || arrayString.isEmpty()) {
            return ifEmpty;
        }

        arrayString = arrayString.trim();
        if (arrayString.startsWith("[") && arrayString.endsWith("]")) {
            arrayString = arrayString.substring(1, arrayString.length() - 1);
        }

        String[] pairs = arrayString.split("\\s*,\\s*");
        for (String pair : pairs) {
            if (!pair.contains("="))
                continue;

            String[] keyValue = pair.split("=", 2);
            map.put(keyValue[0], keyValue[1]);
        }

        if(map.isEmpty()) {
            return ifEmpty;
        }

        return map;
    }

    public String generateArray(String path, String track, Map<String, String> data) {
        Map<String, String> existingData = readArray(path, track, null);
        if (existingData == null) {
            existingData = new HashMap<>();
        }

        existingData.putAll(data);

        StringBuilder sb = new StringBuilder("[");
        Iterator<Map.Entry<String, String>> it = existingData.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();

            sb.append(entry.getKey()).append("=").append(entry.getValue());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append("]");

        return sb.toString();
    }

    public String generateCollection(String path, String type, String track, Map<String, String> data) {
        Map<String, String> existingData = readCollection(path, type, track, null);
        if (existingData == null) {
            existingData = new HashMap<>();
        }

        existingData.putAll(data);

        StringBuilder sb = new StringBuilder("[");
        Iterator<Map.Entry<String, String>> it = existingData.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();

            sb.append(entry.getKey()).append("=").append(entry.getValue());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append("]");

        return sb.toString();
    }

    public Map<String, String> parseCollection(String arrayString, Map<String, String> ifEmpty) {
        Map<String, String> map = new HashMap<>();

        if (arrayString == null || arrayString.isEmpty()) {
            return ifEmpty;
        }

        arrayString = arrayString.trim();
        if (arrayString.startsWith("[") && arrayString.endsWith("]")) {
            arrayString = arrayString.substring(1, arrayString.length() - 1);
        }

        String[] pairs = arrayString.split("\\s*,\\s*");
        for (String pair : pairs) {
            if (!pair.contains("="))
                continue;

            String[] keyValue = pair.split("=", 2);
            map.put(keyValue[0], keyValue[1]);
        }

        if(map.isEmpty()) {
            return ifEmpty;
        }

        return map;
    }

    public void saveCollection(String path, String type, String track, Map<String, String> data) {
        save(path, track, type, generateCollection(path, type, track, data));
    }

    public Map<String, String> readCollection(String path, String type, String track, Map<String, String> ifEmpty) {
        return parseCollection(read(path, track, type, null), ifEmpty);
    }

    public void saveArray(String path, String track, Map<String, String> data) {
        save(path, track, "array", generateArray(path, track, data));
    }

    public Map<String, String> readArray(String path, String track, Map<String, String> ifEmpty) {
        if(path == null)
            return ifEmpty;

        return parseArray(read(path, track, "array", null), ifEmpty);
    }

    public Map<String, List<String>> readAndGetAllItemsByTypeAndTrack(String path,
                                                  String track,
                                                  String type,
                                                  Map<String, List<String>> ifNull) {
        Map<String, List<String>> result = new LinkedHashMap<>();

        try (Stream<String> lines = Files.lines(Path.of(path))) {
            lines
                    .filter(line -> line.contains(type) && line.contains(track))
                    .forEach(line -> {
                        int eqIndex = line.indexOf('=');
                        if (eqIndex == -1) {
                            return;
                        }

                        String header = line.substring(0, eqIndex).trim();

                        int startBracket = line.indexOf('[', eqIndex);
                        int endBracket = line.indexOf(']', startBracket + 1);
                        if (startBracket == -1 || endBracket == -1) {
                            return;
                        }

                        String payload = line.substring(startBracket + 1, endBracket).trim();

                        result.computeIfAbsent(header, k -> new ArrayList<>())
                                .add(payload);
                    });
        } catch (IOException e) {
            e.printStackTrace();
            return ifNull;
        }

        if (result.isEmpty()) {
            return ifNull;
        }

        return result;
    }

    public <T> T read(String path, Predicate<String> trackPredicate, T ifNull) {
        synchronized (fileLock) {
            java.io.File file = new java.io.File(path);

            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                    String line;

                    while ((line = br.readLine()) != null) {
                        line = line.trim();

                        if (trackPredicate.test(line)) {
                            int eqPos = line.indexOf('=');

                            if (eqPos != -1) {
                                int lastSemicolonPos = line.lastIndexOf(';');
                                if (lastSemicolonPos == -1)
                                    lastSemicolonPos = line.length();

                                String dataPart = line.substring(eqPos + 1, lastSemicolonPos).trim();

                                return (T) dataPart;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return ifNull;
        }
    }

    @Override
    public <T> void save(String path, String track, String type, T value) {
        synchronized (fileLock) {
            try {
                isExist(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String valueString;

            if (value != null && value.getClass().isArray()) {
                int length = Array.getLength(value);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < length; i++) {
                    Object element = Array.get(value, i);
                    sb.append(element);
                    if (i < length - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("]");
                valueString = sb.toString();
            } else if (value instanceof Iterable) {
                StringBuilder sb = new StringBuilder("[");
                Iterator<?> it = ((Iterable<?>) value).iterator();
                while (it.hasNext()) {
                    sb.append(it.next());
                    if (it.hasNext()) {
                        sb.append(", ");
                    }
                }
                sb.append("]");
                valueString = sb.toString();
            } else {
                valueString = value.toString();
            }

            try {
                final Path of = Path.of(ResourceManager.Instance.getFullyPath(path));

                if (!Files.exists(of)) {
                    Path parentDir = of.getParent();
                    if (parentDir != null) {
                        Files.createDirectories(parentDir);
                    }
                    Files.createFile(of);
                }

                Path path1 = Paths.get(path);

                int get = getLines(path, line -> {
                    if (track == null) {
                        return line.startsWith(type + " of (") || line.startsWith("array of (" + type + ")");
                    } else {
                        return line.startsWith(type + " of (" + track + ")") || line.startsWith("array of (" + type + ") of (" + track + ")");
                    }
                });

                if (get != 0) {
                    List<String> lines = Files.readAllLines(path1);
                    List<String> newLines = lines.stream()
                            .map(line -> {
                                if (track == null) {
                                    if (line.startsWith(type + " of (") || line.startsWith("array of (" + type + ")")) {
                                        return type + " = " + valueString + ";";
                                    }
                                } else {
                                    if (line.startsWith(type + " of (" + track + ")")) {
                                        return type + " of (" + track + ") = " + valueString + ";";
                                    } else if (line.startsWith("array of (" + type + ") of (" + track + ")")) {
                                        return "array of (" + type + ") of (" + track + ") = " + valueString + ";";
                                    }
                                }
                                return line;
                            }).collect(Collectors.toList());

                    Files.write(path1, newLines);
                } else {
                    try (FileWriter fw = new FileWriter(path, true)) {
                        String prefix;

                        if (value instanceof Iterable) {
                            prefix = "array of (" + type + ")";

                        } else {
                            prefix = type;
                        }

                        if (track != null)
                            prefix += " of (" + track + ")";

                        fw.write(prefix + " = " + valueString + ";" + System.lineSeparator());
                    }
                }
            } catch (IOException e) {
                Music.mainLogger.err(e);
            }
        }
    }

    @Override
    public <T> T read(String path, String track, String type, T ifNull) {
        return read(path, track1 -> track1.startsWith(type + " of (" + track + ")"), ifNull);
    }

    @Override
    public String name(String name) {
        return name + ".txt";
    }
}