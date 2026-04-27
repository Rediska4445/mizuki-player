package rf.ebanina.UI.Editors.Network;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.Network.Net;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists.ListCellTrack;
import rf.ebanina.UI.UI.Element.ListViews.ListView;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.utils.concurrency.LonelyThreadPool;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static rf.ebanina.File.Resources.ResourceManager.getLocaleString;

public class Controller
        implements Initializable
{
    @FXML
    public AnchorPane mainPain;
    @FXML
    public Button searchButton;
    @FXML
    protected TextField search;
    @FXML
    protected Label addictLabel;
    @FXML
    protected Label searchLabel;
    @FXML
    protected Label downloadLabel;
    @FXML
    protected TextField addict;
    @FXML
    protected TextField download;
    @FXML
    protected Label numbers;
    @FXML
    protected Button addToPlaylist;
    @FXML
    protected Button setToPlaylist;

    protected ListView<Track> tracks;

    protected LonelyThreadPool lonelyThreadPool = new LonelyThreadPool();

    protected void search() {
        numbers.setText("");

        lonelyThreadPool.runNewTask(() -> {
            List<Track> res = Net.instance.getListOfTracks(download.getText(), "50", addict.getText());

            Platform.runLater(() -> {
                tracks.getItems().clear();
                tracks.getItems().addAll(res);

                numbers.setText(getLocaleString("quantity", "Quantity") + ": " + tracks.getItems().size());
            });
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tracks = new ListView<>();

        tracks.setId("tracks");
        tracks.setPrefWidth(300);
        tracks.setPrefHeight(100);

        AnchorPane.setBottomAnchor(tracks, 20d);
        AnchorPane.setLeftAnchor(tracks, 340d);
        AnchorPane.setRightAnchor(tracks, 20d);
        AnchorPane.setTopAnchor(tracks, 20d);

        tracks.getStylesheets().setAll(ResourceManager.Instance.loadStylesheet("listview"));
        tracks.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tracks.setCellFactory(lv -> new ListCellTrack<>());

        mainPain.getChildren().add(tracks);

        addToPlaylist.setText(getLocaleString("network_host_add_to_playlist", "add to playlist"));
        setToPlaylist.setText(getLocaleString("network_host_set_to_playlist", "set to playlist"));
        download.setPromptText(getLocaleString("network_host_download_promt_text", "Download"));
        addict.setPromptText(getLocaleString("network_host_addict_promt_text", "Addiction"));

        addict.setText("search");

        mainPain.setStyle("-fx-background-color: #1E1E1E;");

        tracks.setStyle("-fx-background-color: #2D2D2D; -fx-border-color: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + ";");

        addToPlaylist.setStyle("-fx-background-color: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + "; -fx-text-fill: white;");
        setToPlaylist.setStyle("-fx-background-color: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + "; -fx-text-fill: white;");

        addictLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + ";");
        searchLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + ";");
        downloadLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + ";");
        numbers.setStyle("-fx-font-weight: bold; -fx-text-fill: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + ";");
        download.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-prompt-text-fill: #AAAAAA; -jfx-unfocus-color: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + "; -jfx-focus-color: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + ";");
        addict.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-prompt-text-fill: #AAAAAA; -jfx-unfocus-color: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + "; -jfx-focus-color: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + ";");
        search.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-prompt-text-fill: #AAAAAA; -jfx-unfocus-color: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + "; -jfx-focus-color: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + ";");

        numbers.setStyle("-fx-text-fill: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + "; -fx-font-weight: bold;");

        searchButton.setStyle("-fx-background-color: " + ColorProcessor.core.toHex(ColorProcessor.core.getMainClr()) + "; -fx-text-fill: white;");
        searchButton.setOnAction((e) -> search());

        download.setOnKeyReleased(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                search();
            }
        });

        tracks.setOnMouseClicked((e) -> {
            if(e.getButton() == MouseButton.PRIMARY) {
                if(tracks.getSelectionModel().getSelectedItem() != null) {
                    if (!PlayProcessor.playProcessor.isNetwork()) {
                        PlayProcessor.playProcessor.setNetwork(true);

                        PlayProcessor.playProcessor.getTracks().clear();
                        PlayProcessor.playProcessor.getTracks().addAll(tracks.getItems());

                        PlayProcessor.playProcessor.setTrackIter(tracks.getSelectionModel().getSelectedIndex());

                        Root.PlaylistHandler.playlistHandler.playlistSimilar.clear();
                        Root.PlaylistHandler.playlistHandler.playlistSimilar.addAll(PlayProcessor.playProcessor.getTracks());
                    }

                    PlayProcessor.playProcessor.open(tracks.getSelectionModel().getSelectedItem());
                }
            }
        });

        addToPlaylist.setOnAction(e -> Platform.runLater(() -> {
            int point = Root.rootImpl.similar.getTrackListView().getItems().size();

            Root.rootImpl.similar.getTrackListView().getItems().addAll(tracks.getItems());
            Root.PlaylistHandler.playlistHandler.playlistSimilar.addAll(tracks.getItems());

            Root.rootImpl.similar.getTrackListView().getSelectionModel().select(point + Math.max(tracks.getSelectionModel().getSelectedIndex(), 0));
        }));

        setToPlaylist.setOnAction(e -> Platform.runLater(() -> {
            int point = Root.rootImpl.similar.getTrackListView().getItems().size();

            Root.rootImpl.similar.getTrackListView().getItems().setAll(tracks.getItems());
            Root.PlaylistHandler.playlistHandler.playlistSimilar.addAll(tracks.getItems());

            Root.rootImpl.similar.getTrackListView().getSelectionModel().select(point + Math.max(tracks.getSelectionModel().getSelectedIndex(), 0));

            PlayProcessor.playProcessor.getTracks().clear();
            PlayProcessor.playProcessor.getTracks().addAll(Root.rootImpl.similar.getTrackListView().getItems());

            PlayProcessor.playProcessor.setTrackIter(0);
        }));
    }
}
