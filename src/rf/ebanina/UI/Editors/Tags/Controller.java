package rf.ebanina.UI.Editors.Tags;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.File.Resources.Resources;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

//TODO: Переделать под readArray/saveArray
public class Controller implements Initializable {

    public Label title;
    public Button save;
    @FXML
    private ListView<Track.Tag> tagListView;

    @FXML
    private TextField tagInput;

    private Track track;

    private ObservableList<Track.Tag> tagsObservable;

    private final Path tagsDir = Paths.get(Resources.Properties.DEFAULT_CACHE_TRACKS_TAGS_PATH.getKey());

    public void setTrack(Track track) {
        this.track = track;
        loadTags();
    }

    private void loadTags() {
        try {
            Path tagFile = new File(FileManager.instance.name(Resources.Properties.DEFAULT_CACHE_TRACKS_PATH.getKey() + File.separator + track.getPlaylistName())).toPath();

            if (Files.exists(tagFile)) {
                track.deserializeTags(tagFile.toFile());
            }

            tagsObservable = FXCollections.observableArrayList(track.getTags());
            tagListView.setItems(tagsObservable);

            Music.mainLogger.println("[TAGS]: " + track.getTags());

            tagListView.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(Track.Tag item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });

        } catch (IOException | ClassNotFoundException e) {
            Music.mainLogger.err(e);
        }
    }

    @FXML
    private void onAddTag() {
        String newTagName = tagInput.getText().trim();
        if (newTagName.isEmpty()) {
            return;
        }

        for (Track.Tag t : tagsObservable) {
            if (t.getName().equalsIgnoreCase(newTagName)) {
                tagInput.clear();
                return;
            }
        }

        Track.Tag newTag = new Track.Tag().setName(newTagName);
        tagsObservable.add(newTag);
        track.getTags().add(newTag);

        tagInput.clear();
    }

    @FXML
    private void onDeleteSelected() {
        Track.Tag selected = tagListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            tagsObservable.remove(selected);
            track.getTags().remove(selected);
        }
    }

    @FXML
    private void onSave() {
        try {
            track.serializeTags(new File(FileManager.instance.name(Resources.Properties.DEFAULT_CACHE_TRACKS_PATH.getKey() + File.separator + track.getPlaylistName())));
        } catch (IOException e) {
            Music.mainLogger.err(e);
        }

        closeWindow();
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        if(tagListView.getScene() != null) {
            Stage stage = (Stage) tagListView.getScene().getWindow();
            stage.close();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        title.setText(LocalizationManager.getLocaleString("tags_title", "Tags"));
        save.setText(LocalizationManager.getLocaleString("tags_save", "Save tags"));
        tagInput.setPromptText(LocalizationManager.getLocaleString("tags_new_tag", "Save tags"));
    }
}
