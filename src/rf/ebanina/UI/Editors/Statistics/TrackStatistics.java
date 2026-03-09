package rf.ebanina.UI.Editors.Statistics;

import de.umass.lastfm.ImageSize;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.API.Info;
import org.json.simple.parser.ParseException;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.UI.Editors.IEditor;
import rf.ebanina.Network.APIS.SoundCloud;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;
import static rf.ebanina.Network.Illegal.Similar.LastFM.LASTFM_API_KEY;

public class TrackStatistics implements IEditor {
    private Track track;

    public TrackStatistics() {
        track = PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter());
    }

    public TrackStatistics(Track track) {
        this.track = track;
    }

    Tab spotifyTab;
    Tab localTab;
    Tab soundcloudTab;
    Tab lastfmTab;
    Tab itunesTab;

    public static List<Tab> tabs = new ArrayList<>();

    @Override
    public void open(Stage stage) {
        Stage primaryStage = new Stage();
        primaryStage.initOwner(stage);
        primaryStage.initModality(Modality.WINDOW_MODAL);
        primaryStage.setTitle(getLocaleString("track_statistics", "Stats"));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        Label titleLabel = new Label(track.getTitle());
        titleLabel.setFont(ResourceManager.Instance.loadFont("main_font", 20));
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.CENTER);

        VBox topBox = new VBox(titleLabel);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(0, 0, 15, 0));
        root.setTop(topBox);

        TabPane tabPane = new TabPane();

        localTab = new Tab("local");
        localTab.setClosable(false);

        VBox localStatsVbox = new VBox(10);

        TableView<StatItem> statsTable = createStatsTable(
                ResourceManager.Instance.getFullyPath(Resources.Properties.DEFAULT_CACHE_TRACKS_PATH.getKey()
                + File.separator + FileManager.instance.name(track.getPlaylistName())),
                track.getPath()
        );

        localStatsVbox.getChildren().addAll(statsTable);
        localStatsVbox.setPadding(new Insets(10));

        localTab.setContent(localStatsVbox);

        spotifyTab = new Tab("Spotify");
        spotifyTab.setClosable(false);

        soundcloudTab = new Tab("SoundCloud");
        soundcloudTab.setClosable(false);

        lastfmTab = new Tab("LastFM");
        lastfmTab.setClosable(false);

        itunesTab = new Tab("ITunes");
        itunesTab.setClosable(false);

        setContents();

        tabPane.getTabs().addAll(localTab, spotifyTab, soundcloudTab, lastfmTab, itunesTab);
        tabPane.getTabs().addAll(tabs);

        root.setCenter(tabPane);

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private void setContents() {
        executorService.submit(() -> {
            TableView<StatItem> a = createSpotifyTableView();

            Platform.runLater(() -> spotifyTab.setContent(a));
        });

        executorService.submit(() -> {
            TableView<StatItem> a = createSoundCloudTableView();

            Platform.runLater(() -> soundcloudTab.setContent(a));
        });

        executorService.submit(() -> {
            TableView<StatItem> a = createLastFMTableView();

            Platform.runLater(() -> lastfmTab.setContent(a));
        });
    }

    private TableView<StatItem> createLastFMTableView() {
        TableView<StatItem> tableView = createDefaultTable();

        de.umass.lastfm.Track t = de.umass.lastfm.Track.getInfo(
                track.getArtist(),
                track.getTitle(),
                LASTFM_API_KEY
        );

        tableView.getItems().add(new StatItem(getLocaleString("lastfm_track_statistics_title", "title"), t.getName()));
        tableView.getItems().add(new StatItem(getLocaleString("lastfm_track_statistics_artist", "author"), t.getArtist()));
        tableView.getItems().add(new StatItem(getLocaleString("lastfm_track_statistics_album", "location"), t.getAlbum()));
        tableView.getItems().add(new StatItem(getLocaleString("lastfm_track_statistics_location", "album"), t.getLocation()));
        tableView.getItems().add(new StatItem(getLocaleString("lastfm_track_statistics_id", "album"), t.getId()));

        tableView.getItems().add(new StatItem(getLocaleString("lastfm_track_statistics_dura", "album"), String.valueOf(t.getDuration())));
        tableView.getItems().add(new StatItem(getLocaleString("lastfm_track_statistics_play_count", "album"), String.valueOf(t.getUserPlaycount())));
        tableView.getItems().add(new StatItem(getLocaleString("lastfm_track_statistics_tags", "album"), t.getTags().toString()));
        tableView.getItems().add(new StatItem(getLocaleString("lastfm_track_statistics_url", "album"), t.getUrl()));
        tableView.getItems().add(new StatItem(getLocaleString("lastfm_track_statistics_listeners", "album"), String.valueOf(t.getListeners())));

        tableView.getItems().add(new StatItem(getLocaleString("lastfm_track_statistics_image", "album"), String.valueOf(t.getImageURL(ImageSize.MEDIUM))));
        tableView.getItems().add(new StatItem(getLocaleString("lastfm_track_statistics_wiki", "album"), String.valueOf(t.getWikiText())));

        return tableView;
    }

    private TableView<StatItem> createSoundCloudTableView() {
        TableView<StatItem> tableView = createDefaultTable();

        try {
            for(Map.Entry<String, String> a : new SoundCloud().Search(
                            track.viewName()
                    ).getInfo().entrySet()) {
                tableView.getItems().add(new StatItem(getLocaleString("soundcloud_track_statistics_" + a.getKey(), a.getKey()), a.getValue()));
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        return tableView;
    }

    private TableView<StatItem> createSpotifyTableView() {
        TableView<StatItem> tableView = createDefaultTable();

        try {
            me.API.Album.Track track1 = Info.info.search(
                    track.viewName()
            );

            tableView.getItems().add(new StatItem(getLocaleString("spotify_track_statistics_title", "title"), track1.getTitle()));
            tableView.getItems().add(new StatItem(getLocaleString("spotify_track_statistics_author", "author"), track1.getAuthor()));
            tableView.getItems().add(new StatItem(getLocaleString("spotify_track_statistics_art", "art"), track1.getAwesomeAlbumArt().getUrl()));
            tableView.getItems().add(new StatItem(getLocaleString("spotify_track_statistics_popularity", "pop"), String.valueOf(track1.getPopularity())));
            tableView.getItems().add(new StatItem(getLocaleString("spotify_track_statistics_duration", "dura"), String.valueOf(track1.getDuration())));
            tableView.getItems().add(new StatItem(getLocaleString("spotify_track_statistics_explicit", "explicit"), String.valueOf(track1.isExplicit())));
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        return tableView;
    }

    private TableView<StatItem> createDefaultTable() {
        TableView<StatItem> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<StatItem, String> nameColumn = new TableColumn<>(getLocaleString("track_statistics_key", "key"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("statName"));

        TableColumn<StatItem, String> valueColumn = new TableColumn<>(getLocaleString("track_statistics_value", "key"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("statValue"));

        tableView.getSelectionModel().setCellSelectionEnabled(true);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tableView.getColumns().addAll(nameColumn, valueColumn);

        return tableView;
    }

    private TableView<StatItem> createStatsTable(String path, String trackName) {
        TableView<StatItem> tableView = createDefaultTable();

        List<StatItem> stats = collectStats(path, trackName);
        tableView.getItems().addAll(stats);

        return tableView;
    }

    private List<StatItem> collectStats(String path, String track) {
        List<StatItem> list = new ArrayList<>();

        for(Map.Entry<String, String> entry : FileManager.instance.readArray(path, track, Map.of()).entrySet()) {
            list.add(new StatItem(getLocaleString("track_statistics_" + entry.getKey(), entry.getKey()), entry.getValue()));
        }

        return list;
    }

    public String getPath() {
        return track.toString();
    }

    public static class StatItem {
        private final String statName;
        private final String statValue;

        public StatItem(String statName, String statValue) {
            this.statName = statName;
            this.statValue = statValue;
        }

        public String getStatName() {
            return statName;
        }

        public String getStatValue() {
            return statValue;
        }

        @Override
        public String toString() {
            return "StatItem{" +
                    "statName='" + statName + '\'' +
                    ", statValue='" + statValue + '\'' +
                    '}';
        }
    }
}
