package rf.ebanina.UI.Network;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists.ListCellSimilar;
import rf.ebanina.UI.UI.Element.ListViews.ListView;

import java.util.List;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;
import static rf.ebanina.Network.Info.getTracks;

public class Controller {
    public AnchorPane mainPain;
    public JFXTextField search;
    @FXML private TextField addict;
    @FXML private TextField download;
    @FXML private Label numbers;
    @FXML private JFXButton addToPlaylist;
    @FXML private JFXButton setToPlaylist;
    private ListView<Track> tracks;

    @FXML
    private void initialize() {
        tracks = new ListView<>();
        tracks.setLayoutX(340);
        tracks.setLayoutY(20);
        tracks.setPrefWidth(500);
        tracks.setPrefHeight(560);
        tracks.setId("tracks");
        tracks.setStyle("-fx-background-color: white; -fx-border-color: #7092be;");

        AnchorPane.setBottomAnchor(tracks, 20d);
        AnchorPane.setLeftAnchor(tracks, 340d);
        AnchorPane.setRightAnchor(tracks, 20d);
        AnchorPane.setTopAnchor(tracks, 20d);

        tracks.getStylesheets().setAll(ResourceManager.Instance.loadStylesheet("listview"));
        tracks.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tracks.setCellFactory(lv -> new ListCellSimilar());

        mainPain.getChildren().add(tracks);

        addToPlaylist.setText(getLocaleString("network_host_add_to_playlist", "add to playlist"));
        setToPlaylist.setText(getLocaleString("network_host_set_to_playlist", "set to playlist"));
        download.setPromptText(getLocaleString("network_host_download_promt_text", "Download"));
        addict.setPromptText(getLocaleString("network_host_addict_promt_text", "Addiction"));

        addict.setText("search");

        download.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                numbers.setText("");

                new Thread(() -> {
                    List<Track> res = getTracks.getTrack(download.getText(), "200", addict.getText());

                    Music.mainLogger.println(res);

                    Platform.runLater(() -> {
                        tracks.getItems().clear();
                        tracks.getItems().addAll(res);

                        numbers.setText(getLocaleString("quantity", "Quantity") + ": " + tracks.getItems().size());
                    });
                }).start();
            }
        });

        tracks.setOnMouseClicked((e) -> {
            if(e.getButton() == MouseButton.PRIMARY) {
                if(!PlayProcessor.playProcessor.isNetwork()) {
                    PlayProcessor.playProcessor.setNetwork(true);

                    PlayProcessor.playProcessor.getTracks().clear();
                    PlayProcessor.playProcessor.getTracks().addAll(tracks.getItems());

                    PlayProcessor.playProcessor.setTrackIter(tracks.getSelectionModel().getSelectedIndex());

                    Root.similar.getTrackListView().getItems().clear();
                    Root.similar.getTrackListView().getItems().addAll(PlayProcessor.playProcessor.getTracks());
                    Root.similar.getTrackListView().getSelectionModel().select(PlayProcessor.playProcessor.getTrackIter());

                    Root.PlaylistHandler.playlistSimilar.clear();
                    Root.PlaylistHandler.playlistSimilar.addAll(PlayProcessor.playProcessor.getTracks());
                }

                MediaProcessor.mediaProcessor.regenerateMediaPlayer(tracks.getSelectionModel().getSelectedItem());

                // OnlineTrack.play(tracks.getSelectionModel().getSelectedItem());
            }
        });

        addToPlaylist.setOnAction(e -> Platform.runLater(() -> {
            int point = Root.similar.getTrackListView().getItems().size();

            Root.similar.getTrackListView().getItems().addAll(tracks.getItems());
            Root.PlaylistHandler.playlistSimilar.addAll(tracks.getItems());

            Root.similar.getTrackListView().getSelectionModel().select(point + Math.max(tracks.getSelectionModel().getSelectedIndex(), 0));
        }));

        setToPlaylist.setOnAction(e -> Platform.runLater(() -> {
            int point = Root.similar.getTrackListView().getItems().size();

            Root.similar.getTrackListView().getItems().setAll(tracks.getItems());
            Root.PlaylistHandler.playlistSimilar.addAll(tracks.getItems());

            Root.similar.getTrackListView().getSelectionModel().select(point + Math.max(tracks.getSelectionModel().getSelectedIndex(), 0));

            PlayProcessor.playProcessor.getTracks().clear();
            PlayProcessor.playProcessor.getTracks().addAll(Root.similar.getTrackListView().getItems());
            PlayProcessor.playProcessor.setTrackIter(0);
        }));
    }
}
