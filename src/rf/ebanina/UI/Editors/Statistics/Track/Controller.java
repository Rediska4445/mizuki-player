package rf.ebanina.UI.Editors.Statistics.Track;

import de.umass.lastfm.ImageSize;
import deezer.Deezer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import me.API.Info;
import org.json.simple.parser.ParseException;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.Network.Illegal.Similar.SoundCloud;
import rf.ebanina.Network.Net;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.utils.loggining.logging;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static rf.ebanina.File.Resources.ResourceManager.getLocaleString;
import static rf.ebanina.Network.Illegal.Similar.LastFM.LASTFM_API_KEY;

@logging(tag = "TrackStatisticsController")
public class Controller
        implements Initializable
{
    @FXML
    protected Label titleLabel;

    @FXML
    protected TabPane tabPane;

    @FXML
    private TableView<StatItem> localStatsTable, spotifyTable, soundcloudTable, lastfmTable, itunesTable, deezerTable;
    @FXML
    private TableColumn<StatItem, String> localNameColumn, localValueColumn;
    @FXML
    private TableColumn<StatItem, String> spotifyNameColumn, spotifyValueColumn;
    @FXML
    private TableColumn<StatItem, String> soundcloudNameColumn, soundcloudValueColumn;
    @FXML
    private TableColumn<StatItem, String> lastfmNameColumn, lastfmValueColumn;
    @FXML
    private TableColumn<StatItem, String> itunesNameColumn, itunesValueColumn;
    @FXML
    private TableColumn<StatItem, String> deezerNameColumn, deezerValueColumn;

    protected volatile Track track;

    public Controller setTrack(Track track) {
        this.track = track;
        return this;
    }

    protected final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public Label getTitleLabel() {
        return titleLabel;
    }

    public Controller setTitleLabel(Label titleLabel) {
        this.titleLabel = titleLabel;
        return this;
    }

    public TabPane getTabPane() {
        return tabPane;
    }

    public Controller setTabPane(TabPane tabPane) {
        this.tabPane = tabPane;
        return this;
    }

    public Track getTrack() {
        return track;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTables();
    }

    public void initializeData() {
        if (this.track == null) {
            this.track = PlayProcessor.playProcessor.getCurrentTrack();
        }

        applyColorScheme();

        setupLocalTab();
        setContents();
    }

    public void applyColorScheme(TableView<Controller.StatItem> localStatsTable) {
        String bgTable = "#333333";
        String borderColor = "#444444";

        String tableStyle = "-fx-background-color: " + bgTable + "; " +
                "-fx-control-inner-background: " + bgTable + "; " +
                "-fx-table-cell-border-color: " + borderColor + "; " +
                "-fx-border-color: " + borderColor + "; " +
                "-fx-border-radius: 8; -fx-background-radius: 8;";

        localStatsTable.setStyle(tableStyle);
    }

    private void applyColorScheme() {
        String hexColor = ColorProcessor.core.toHex(ColorProcessor.core.getGeneralColorFromImage(track.getAlbumArt()));
        String bgDark = "#1E1E1E";
        String bgTable = "#333333";
        String borderColor = "#444444";

        String tableStyle = "-fx-background-color: " + bgTable + "; " +
                "-fx-control-inner-background: " + bgTable + "; " +
                "-fx-table-cell-border-color: " + borderColor + "; " +
                "-fx-border-color: " + borderColor + "; " +
                "-fx-border-radius: 8; -fx-background-radius: 8;";

        localStatsTable.setStyle(tableStyle);
        spotifyTable.setStyle(tableStyle);
        deezerTable.setStyle(tableStyle);
        soundcloudTable.setStyle(tableStyle);
        lastfmTable.setStyle(tableStyle);
        itunesTable.setStyle(tableStyle);

        for(TableView<Controller.StatItem> table : List.of(
                localStatsTable, spotifyTable, deezerTable, soundcloudTable, lastfmTable, itunesTable
        )) {
            applyColorScheme(table);
        }

        tabPane.getStylesheets().add(ResourceManager.getInstance().loadStylesheet("tabpane"));
        tabPane.setStyle("-fx-accent-color: " + hexColor + "; " +
                "-fx-background-color: " + bgDark + "; " +
                "-fx-tab-pane-background: " + bgDark + ";");

        String titleStyle = "-fx-font-weight: bold; -fx-text-fill: " + hexColor + "; -fx-font-size: 20px;";
        titleLabel.setStyle(titleStyle);
    }

    protected void setupTables() {
        setupTableColumns(localStatsTable, localNameColumn, localValueColumn);
        setupTableColumns(spotifyTable, spotifyNameColumn, spotifyValueColumn);
        setupTableColumns(soundcloudTable, soundcloudNameColumn, soundcloudValueColumn);
        setupTableColumns(lastfmTable, lastfmNameColumn, lastfmValueColumn);
        setupTableColumns(itunesTable, itunesNameColumn, itunesValueColumn);
        setupTableColumns(deezerTable, deezerNameColumn, deezerValueColumn);
    }

    protected void setupTableColumns(TableView<StatItem> table,
                                   TableColumn<StatItem, String> nameCol,
                                   TableColumn<StatItem, String> valueCol) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        nameCol.setCellValueFactory(new PropertyValueFactory<>("statName"));
        valueCol.setCellValueFactory(new PropertyValueFactory<>("statValue"));

        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    protected void setupLocalTab() {
        String path = ResourceManager.getInstance().getFullyPath(
                Resources.Properties.DEFAULT_CACHE_TRACKS_PATH.getKey() +
                        File.separator + FileManager.getInstance().name(track.getPlaylistName())
        );

        List<StatItem> stats = collectStats(path, track.getPath());
        localStatsTable.getItems().addAll(stats);
    }

    protected void setContents() {
        titleLabel.setText(track.getTitle());
        titleLabel.setFont(ResourceManager.Instance.loadFont("main_font", 20));

        executorService.submit(() -> loadRemoteStats("Spotify", this::createSpotifyTableView, spotifyTable));
        executorService.submit(() -> loadRemoteStats("Deezer", this::createDeezerTableView, deezerTable));
        executorService.submit(() -> loadRemoteStats("SoundCloud", this::createSoundCloudTableView, soundcloudTable));
        executorService.submit(() -> loadRemoteStats("LastFM", this::createLastFMTableView, lastfmTable));
    }

    private void loadRemoteStats(String service, Supplier<TableView<StatItem>> loader,
                                 TableView<StatItem> targetTable) {
        try {
            TableView<StatItem> table = loader.get();
            Platform.runLater(() -> targetTable.getItems().addAll(table.getItems()));
        } catch (Exception e) {
            Platform.runLater(() -> targetTable.getItems().add(
                    new StatItem("Error", String.format("Failed to load %s data", service))));
        }
    }

    private TableView<StatItem> createLastFMTableView() {
        TableView<StatItem> tableView = createDefaultTable();
        de.umass.lastfm.Track t = de.umass.lastfm.Track.getInfo(
                track.getArtist(), track.getTitle(), LASTFM_API_KEY
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
            for(Map.Entry<String, String> a : SoundCloud.sc.Search(track.viewName()).getInfo().entrySet()) {
                tableView.getItems().add(new StatItem(
                        getLocaleString("soundcloud_track_statistics_" + a.getKey(), a.getKey()),
                        a.getValue()
                ));
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        return tableView;
    }

    private TableView<StatItem> createDeezerTableView() {
        TableView<StatItem> tableView = createDefaultTable();

        Net.getInstance().<Deezer>getOriginalFromSimilarMap(Net.PlayersTypes.DEEZER.getCode()).ifPresent(obj -> {
            try {
                for (deezer.models.Track deezerTrack : obj.searchTracks(track.viewName(), 1)) {
                    tableView.getItems().add(new StatItem(getLocaleString("spotify_track_statistics_title", "title"), deezerTrack.getTitle()));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_author", "author"), deezerTrack.getArtist().getName()));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_isrc", "isrc"), deezerTrack.getIsrc()));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_link", "link"), deezerTrack.getLink()));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_type", "type"), deezerTrack.getType()));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_art", "art"), deezerTrack.getArtist().getPicture()));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_tracklist", "tracklist"), deezerTrack.getArtist().getTracklist()));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_type", "type"), deezerTrack.getArtist().getType()));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_album", "album"), deezerTrack.getAlbum().getTitle()));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_art", "art"), deezerTrack.getAlbum().getCover_xl()));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_duration", "duration"), String.valueOf(deezerTrack.getDuration())));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_popularity", "popularity"), String.valueOf(deezerTrack.getRank())));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_explicit", "explicit"), String.valueOf(deezerTrack.getExplicit_content_cover())));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_explicit", "explicit"), String.valueOf(deezerTrack.getExplicit_content_lyrics())));
                    tableView.getItems().add(new StatItem(getLocaleString("deezer_track_statistics_id", "id"), String.valueOf(deezerTrack.getId())));
                }
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        });

        return tableView;
    }

    private TableView<StatItem> createSpotifyTableView() {
        TableView<StatItem> tableView = createDefaultTable();

        try {
            me.API.Album.Track track1 = Info.info.search(track.viewName());
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

        tableView.getColumns().addAll(nameColumn, valueColumn);

        return tableView;
    }

    private List<StatItem> collectStats(String path, String trackName) {
        List<StatItem> list = new ArrayList<>();

        for(Map.Entry<String, String> entry : FileManager.instance.readArray(path, trackName, Map.of()).entrySet()) {
            list.add(new StatItem(getLocaleString("track_statistics_" + entry.getKey(), entry.getKey()), entry.getValue()));
        }

        return list;
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
            return "StatItem{" + "statName='" + statName + '\'' + ", statValue='" + statValue + '\'' + '}';
        }
    }
}