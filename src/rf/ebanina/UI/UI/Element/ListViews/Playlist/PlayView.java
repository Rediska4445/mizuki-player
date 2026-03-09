package rf.ebanina.UI.UI.Element.ListViews.Playlist;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Playlist;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.Buttons.Playlist.NextPlaylistButton;
import rf.ebanina.UI.UI.Element.Buttons.Playlist.PlaylistButton;
import rf.ebanina.UI.UI.Element.Buttons.Playlist.PrevPlaylistButton;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.AnimatedListCell;
import rf.ebanina.UI.UI.Element.ListViews.ListView;
import rf.ebanina.UI.UI.Element.Text.TextField;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.Network.ISimilar;
import rf.ebanina.Network.Info;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static rf.ebanina.Network.Info.getTracks;
import static rf.ebanina.Network.Info.similarList;

public class PlayView<T extends Track, J extends Playlist> extends VBox {
    private StackPane centerStack;
    private ListView<T> trackListView;
    private ListView<J> playlistListView;

    private TextField searchBar;
    private TextField currentPlaylistText;

    private BorderPane topPane;
    private BorderPane bottomPane;

    private rf.ebanina.UI.UI.Element.Buttons.Button btnPlaylist;
    private Button btnPlaylistNext;
    private Button btnPlaylistDown;

    private final DoubleProperty topHeight = new SimpleDoubleProperty(this, "topHeight", 60);
    private final DoubleProperty bottomHeight = new SimpleDoubleProperty(this, "bottomHeight", 50);

    private final ExecutorService search = Executors.newSingleThreadExecutor();

    private PlayProcessor<T, J> playProcessor;

    public PlayProcessor<T, J> getPlayProcessor() {
        return playProcessor;
    }

    public PlayView<T, J> setPlayProcessor(PlayProcessor<T, J> playProcessor) {
        this.playProcessor = playProcessor;
        return this;
    }

    private List<T> searchByTags(String query, List<T> tracks) {
        String normalized = query.replaceAll("\\s*\\|\\s*", "|").replaceAll("\\s*&\\s*", "&").trim();
        String[] orParts = normalized.split("\\|");

        List<T> result = new ArrayList<>();

        for (T track : tracks) {
            List<String> trackTagsLower = track.getTags() == null ? Collections.emptyList() :
                    track.getTags().stream()
                            .map(Track.Tag::getName)
                            .filter(Objects::nonNull)
                            .map(String::toLowerCase)
                            .map(tagName -> tagName.startsWith("#") ? tagName.substring(1) : tagName)
                            .toList();

            boolean matchesOr = false;

            for (String orPart : orParts) {
                String[] andParts = orPart.split("&");
                boolean matchesAnd = true;

                for (String andPart : andParts) {
                    String tag = andPart.trim();

                    if (tag.startsWith("#")) {
                        tag = tag.substring(1).toLowerCase();
                    } else {
                        tag = tag.toLowerCase();
                    }

                    String finalTag = tag;
                    boolean found = trackTagsLower.stream().anyMatch(t -> t.contains(finalTag));
                    if (!found) {
                        matchesAnd = false;
                        break;
                    }
                }

                if (matchesAnd) {
                    matchesOr = true;
                    break;
                }
            }

            if (matchesOr) {
                result.add(track);
            }
        }

        return result;
    }

    private static class LogicalCondition {
        public enum Operator {
            AND,
            OR
        }

        public LogicalCondition.Operator operator;
        public List<String> conditions;

        public LogicalCondition(LogicalCondition.Operator operator, List<String> conditions) {
            this.operator = operator;
            this.conditions = conditions;
        }
    }

    private LogicalCondition parseLogicalConditions(String input) {
        input = input.trim();
        if (!input.endsWith("!")) {
            return null;
        }

        input = input.substring(0, input.length() - 1).trim();

        if (input.contains("&&")) {
            String[] parts = input.split("\\s*&&\\s*");
            return new LogicalCondition(LogicalCondition.Operator.AND, Arrays.asList(parts));
        } else if (input.contains("||")) {
            String[] parts = input.split("\\s*\\|\\|\\s*");
            return new LogicalCondition(LogicalCondition.Operator.OR, Arrays.asList(parts));
        } else {
            return new LogicalCondition(null, Collections.singletonList(input));
        }
    }

    private List<T> filterByKeyValue(String key, List<T> source, Function<T, String> getProperty) {
        LogicalCondition condition = parseLogicalConditions(key);

        if (condition == null)
            return source;

        if (condition.operator == LogicalCondition.Operator.AND) {
            List<T> filtered = source;

            for (String cond : condition.conditions) {
                filtered = filterSingleCondition(cond, filtered, getProperty);
            }

            return filtered;
        } else if (condition.operator == LogicalCondition.Operator.OR) {
            Set<T> resultSet = new HashSet<>();

            for (String cond : condition.conditions) {
                resultSet.addAll(filterSingleCondition(cond, source, getProperty));
            }

            return new ArrayList<>(resultSet);
        } else {
            return filterSingleCondition(condition.conditions.get(0), source, getProperty);
        }
    }

    private List<T> filterSingleCondition(String condition, List<T> source, Function<T, String> getProperty) {
        int eqIndex = condition.indexOf('=');
        String valueToFind = eqIndex == -1 ? condition.trim().toLowerCase() : condition.substring(eqIndex + 1).trim().toLowerCase();

        return source.stream()
                .filter(track -> {
                    String prop = getProperty.apply(track);
                    return prop != null && prop.toLowerCase().contains(valueToFind);
                }).collect(Collectors.toList());
    }

    public String valueProcess(String key) {
        if (!key.trim().endsWith("!"))
            return null;

        String query = key.trim();

        return query.substring(0, query.length() - 1).trim();
    }

    public final List<Query<T>> queryTypes = new ArrayList<>(List.of(
            new Query<>() {
                @Override
                public String tag() {
                    return "@author";
                }

                @Override
                public List<T> search(String key, List<T> source) {
                    return filterByKeyValue(key, source, Track::getArtist);
                }
            },
            new Query<>() {
                @Override
                public String tag() {
                    return "@title";
                }

                @Override
                public List<T> search(String key, List<T> source) {
                    return filterByKeyValue(key, source, Track::getTitle);
                }
            },
            new Query<>() {
                @Override
                public String tag() {
                    return "@extension";
                }

                @Override
                public List<T> search(String key, List<T> source) {
                    return filterByKeyValue(key, source, Track::getExtension);
                }
            },
            new Query<>() {
                @Override
                public String tag() {
                    return "@dura";
                }

                @Override
                public List<T> search(String key, List<T> source) {
                    return filterByKeyValue(key, source, track -> String.valueOf(track.getDuration()));
                }
            },
            new Query<>() {
                @Override
                public String tag() {
                    return "@inet";
                }

                @Override
                public List<T> search(String key, List<T> source) {
                    String query = valueProcess(key);

                    if (query == null)
                        return source;

                    int eqIndex = query.indexOf('=');

                    if (eqIndex == -1)
                        return source;

                    List<Track> foundTrack = getTracks.getTrack("kute", "50", "search");

                    if (foundTrack == null) {
                        return Collections.emptyList();
                    } else {
                        return Collections.singletonList((T) foundTrack);
                    }
                }
            },
            new Query<>() {
                @Override
                public String tag() {
                    return "@similar";
                }

                @Override
                public List<T> search(String key, List<T> source) {
                    String query = valueProcess(key);

                    if (query == null)
                        return source;

                    int eqIndex = query.indexOf('=');

                    if (eqIndex == -1)
                        return source;

                    List<Track> res = new ArrayList<>();

                    for (ISimilar i : similarList) {
                        res.addAll(i.getSimilar(query));
                    }

                    return Collections.singletonList((T) res);
                }
            },
            new Query<>() {
                @Override
                public String tag() {
                    return "@playlist";
                }

                //TODO: Переделать, работает очень долго
                @Override
                public List<T> search(String key, List<T> source) {
                    String query = valueProcess(key);

                    if(query == null)
                        return source;

                    int eqIndex = query.indexOf('=');

                    if (eqIndex == -1)
                        return source;

                    List<Track> t = new ArrayList<>();

                    String playlistPart = query.substring(eqIndex + 1).trim().replace(" ", "");

                    String[] parts = playlistPart.split("[|&]");

                    List<Playlist> playlists = new ArrayList<>();

                    if(playlistPart.equalsIgnoreCase("all")) {
                        playlists.addAll(PlayProcessor.playProcessor.getCurrentPlaylist());
                    } else {
                        for(String part : parts) {
                            playlists.add(new Playlist(part));
                        }
                    }

                    for(Playlist playlistF : playlists) {
                        String playlistName = playlistF.getName();

                        Playlist playlist = new Playlist();

                        Optional<Playlist> playlistOp = PlayProcessor.playProcessor.getCurrentPlaylist().stream().filter(e -> e.getPath().endsWith(playlistName)).findFirst();

                        if (playlistOp.isPresent()) {
                            playlist = playlistOp.get();
                        }

                        try (Stream<Path> stream = Files.walk(Path.of(playlist.getPath()), FOLLOW_LINKS)) {
                            stream
                                    .filter(Files::isRegularFile)
                                    .filter(t1 -> FileManager.instance.hasSupportedExtension(t1))
                                    .forEach(file -> {
                                        Track track = new Track(file.toAbsolutePath().toString());
                                        t.add(track);
                                    });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    return (List<T>) t;
                }
            }
    ));

    private List<T> queryTypesProcess(String inputQuery) {
        if (inputQuery == null || !inputQuery.trim().endsWith("!")) {
            return null;
        }

        String query = inputQuery.trim();
        query = query.substring(0, query.length() - 1).trim();

        List<T> filteredTracks = new ArrayList<>(playProcessor.getTracks());

        String[] annotations = query.split("\\s+(?=@)");

        for (String annotation : annotations) {
            annotation = annotation.trim();

            String key = annotation.contains("=") ? annotation.substring(0, annotation.indexOf("=")).trim() : annotation;

            Optional<Query<T>> queryImpl = queryTypes.stream()
                    .filter(q -> q.tag().equalsIgnoreCase(key))
                    .findFirst();

            if (queryImpl.isPresent()) {
                filteredTracks = queryImpl.get().search(annotation + " !", filteredTracks);
            } else {
                String filterStr = annotation.replace("@", "").split("=")[0].toLowerCase();
                filteredTracks = filteredTracks.stream()
                        .filter(t -> t.viewName.toLowerCase().contains(filterStr))
                        .collect(Collectors.toList());
            }
        }

        return filteredTracks;
    }

    private List<T> search(String searchWords, List<T> listOfStrings) {
        List<String> searchWordsArray = Arrays.asList(searchWords.trim().split(" "));

        if (searchWords.startsWith("#")) {
            return searchByTags(searchWords, listOfStrings);
        } else if(searchWords.startsWith("@")) {
            List<T> t = queryTypesProcess(searchWords);

            if(t == null) {
                return listOfStrings;
            } else {
                return t;
            }
        } else {
            return (listOfStrings.stream().filter(input -> searchWordsArray.stream().allMatch(word -> {
                if(input.getPath().equals(Info.PlayersTypes.URI_NULL.getCode()))
                    return input.viewName.toLowerCase().contains(word.toLowerCase());
                else
                    return input.toString().toLowerCase().contains(word.toLowerCase());
            })).collect(Collectors.toList()));
        }
    }

    private javafx.event.EventHandler<? super javafx.scene.input.KeyEvent> searchHandler = (t1) -> search.submit(() -> {
        if(trackListView.isVisible()) {
            String searchWords = searchBar.getText();

            List<T> res = new ArrayList<>(search(searchWords, playProcessor.getTracks()));

            if (res.size() == 0) {
                List<T> temp = new ArrayList<>();

                try (Stream<Path> stream = Files.walk(Paths.get(PlayProcessor.playProcessor.getCurrentDefaultMusicDir()), FOLLOW_LINKS)) {
                    stream.forEach(file -> temp.add((T) new Track(file.toFile().getPath())));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                res.addAll(search(searchWords, temp));
            }

            Platform.runLater(() -> {
                trackListView.getItems().clear();

                for (T re : res) {
                    trackListView.getItems().add(re);
                }

                if (searchBar.getText().isEmpty()) {
                    trackListView.getSelectionModel().select(playProcessor.getTrackIter());
                }
            });
        } else {
            AtomicReference<List<J>> res = new AtomicReference<>();

            List<String> searchWordsArray = Arrays.asList(searchBar.getText().trim().split(" "));

            res.set(playProcessor.getCurrentPlaylist().stream().filter(input -> searchWordsArray.stream().allMatch(word ->
                    input.getPath().toLowerCase().contains(word.toLowerCase()))).collect(Collectors.toList()));

            Platform.runLater(() -> {
                playlistListView.getItems().clear();

                for (J re : res.get()) {
                    playlistListView.getItems().add(re);
                }

                if (searchBar.getText().isEmpty()) {
                    playlistListView.getSelectionModel().select(playProcessor.getTrackIter());
                }
            });
        }
    });

    public PlayView() {
        this(ResourceManager.Instance.loadStylesheet("listview"));
    }

    public PlayView(String style) {
        super(0);

        trackListView = new ListView<>();
        playlistListView = new ListView<>();

        trackListView.setCellFactory((e) -> new AnimatedListCell<>(Color.BLACK) {
            @Override protected void onItemDropped(int draggedIndex, int targetIndex) {

            }
        });

        playlistListView.setCellFactory((e) -> new AnimatedListCell<>(Color.BLACK) {
            @Override protected void onItemDropped(int draggedIndex, int targetIndex) {

            }
        });

        trackListView.getStylesheets().addAll(style);
        playlistListView.getStylesheets().addAll(style);

        playlistListView.prefWidthProperty().bind(trackListView.prefWidthProperty());
        playlistListView.prefHeightProperty().bind(trackListView.prefHeightProperty());
        playlistListView.visibleProperty().bind(trackListView.visibleProperty().not());
        playlistListView.disableProperty().bind(trackListView.disableProperty().not());

        currentPlaylistText = new TextField();
        currentPlaylistText.setFocusColor(Color.TRANSPARENT);
        currentPlaylistText.setUnFocusColor(Color.TRANSPARENT);
        currentPlaylistText.setAlignment(Pos.CENTER);
        currentPlaylistText.setDisableAnimation(true);

        searchBar = new TextField();
        searchBar.setBackground(Background.EMPTY);
        searchBar.setUnFocusColor(ColorProcessor.core.getMainClr());
        searchBar.setFocusColor(Color.TRANSPARENT);

        topPane = new BorderPane();
        topPane.setTop(currentPlaylistText);
        topPane.setCenter(searchBar);
        topPane.setPadding(new Insets(5));

        topPane.prefHeightProperty().bind(topHeight);
        topPane.setMaxHeight(Double.MAX_VALUE);

        btnPlaylistDown = new PrevPlaylistButton();
        btnPlaylist = new PlaylistButton();
        btnPlaylistNext = new NextPlaylistButton();

        btnPlaylistDown.setCursor(Cursor.HAND);
        btnPlaylist.setCursor(Cursor.HAND);
        btnPlaylistNext.setCursor(Cursor.HAND);

        bottomPane = new BorderPane();
        bottomPane.setLeft(btnPlaylistDown);
        bottomPane.setCenter(btnPlaylist);
        bottomPane.setRight(btnPlaylistNext);

        setMargin(bottomPane, new Insets(15, 0, 0, 0));

        bottomPane.prefHeightProperty().bind(bottomHeight);
        bottomPane.setMaxHeight(Double.MAX_VALUE);

        centerStack = new StackPane(trackListView, playlistListView);
        VBox.setVgrow(centerStack, Priority.ALWAYS);

        getChildren().addAll(topPane, centerStack, bottomPane);
        setPadding(new Insets(5));

        topPane.setMinHeight(Region.USE_PREF_SIZE);
        bottomPane.setMinHeight(Region.USE_PREF_SIZE);

        trackListView.setMinHeight(Region.USE_PREF_SIZE);
        trackListView.setMaxWidth(Double.MAX_VALUE);
        trackListView.setPrefHeight(Region.USE_COMPUTED_SIZE);

        playlistListView.setMinHeight(Region.USE_PREF_SIZE);
        playlistListView.setMaxWidth(Double.MAX_VALUE);

        currentPlaylistText.setFont(Font.font("System", FontWeight.NORMAL, 11));
        searchBar.setFont(Font.font("System", FontWeight.NORMAL, 11));

        searchBar.setOnKeyReleased(searchHandler);
    }

    public PlayView<T, J> setSearchHandler(EventHandler<? super KeyEvent> searchHandler) {
        this.searchHandler = searchHandler;
        return this;
    }

    public final boolean isOpened() {
        return isVisible();
    }

    public final void open() {
        setVisible(true);
        setDisable(false);
    }

    public final void close() {
        setVisible(false);
        setDisable(true);
    }

    public final void openTrackList() {
        this.trackListView.setDisable(false);
        this.trackListView.setVisible(true);
    }

    public final void closeTrackList() {
        this.trackListView.setDisable(true);
        this.trackListView.setVisible(false);
    }

    public final double getTopHeight() {
        return topHeight.get();
    }

    public final void setTopHeight(double value) {
        topHeight.set(value);
    }

    public final DoubleProperty topHeightProperty() {
        return topHeight;
    }

    public final double getBottomHeight() {
        return bottomHeight.get();
    }

    public final void setBottomHeight(double value) {
        bottomHeight.set(value);
    }

    public final DoubleProperty bottomHeightProperty() {
        return bottomHeight;
    }

    public TextField getSearchBar() {
        return searchBar;
    }

    public TextField getCurrentPlaylistText() {
        return currentPlaylistText;
    }

    public BorderPane getTopPane() {
        return topPane;
    }

    public BorderPane getBottomPane() {
        return bottomPane;
    }

    public rf.ebanina.UI.UI.Element.Buttons.Button getBtnPlaylist() {
        return btnPlaylist;
    }

    public Button getBtnPlaylistNext() {
        return btnPlaylistNext;
    }

    public Button getBtnPlaylistDown() {
        return btnPlaylistDown;
    }

    public ObjectProperty<Color> getTracksSelectedColorProperty() {
        return trackListView.getSelectedColorProperty();
    }

    public ObjectProperty<Color> getPlaylistSelectedColorProperty() {
        return playlistListView.getSelectedColorProperty();
    }

    public ListView<T> getTrackListView() {
        return trackListView;
    }

    public ListView<J> getPlaylistListView() {
        return playlistListView;
    }

    public StackPane getCenter() {
        return centerStack;
    }
}