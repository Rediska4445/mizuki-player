package rf.ebanina.UI.Editors.Tags;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Track;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

//TODO: Переделать под readArray/saveArray
public class Controller implements Initializable {

    public Label title;
    public Button save;
    public Button addButton;
    public Button removeButton;
    public VBox vBox;
    @FXML
    private ListView<Track.Tag> tagListView;

    @FXML
    private TextField tagInput;

    private Track track;

    private ObservableList<Track.Tag> tagsObservable;

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

        String hexColor = ColorProcessor.core.getGeneralColorFromImage(track.getAlbumArt()).toString().replace("0x", "#");

        // Заголовок
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + hexColor + ";");

        // Кнопка сохранения
        save.setStyle("-fx-background-color: " + hexColor + "; -fx-text-fill: white; -fx-font-weight: bold;");

        // Кнопки +/-
        addButton.setStyle("-fx-background-color: " + hexColor + "; -fx-text-fill: white; -fx-font-size: 14px;");
        removeButton.setStyle("-fx-background-color: " + hexColor + "; -fx-text-fill: white; -fx-font-size: 14px;");

        // Поле ввода
        tagInput.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-prompt-text-fill: #AAAAAA; -fx-border-color: " + hexColor + "; -fx-focus-color: " + hexColor + ";");
        tagInput.setPromptText(LocalizationManager.getLocaleString("tags_new_tag", "Введите новый тег"));

        // Список тегов
        tagListView.setStyle("-fx-background-color: #2D2D2D; -fx-border-color: " + hexColor + "; -fx-control-inner-background: #2D2D2D;");

        // Основной контейнер
        vBox.setStyle("-fx-background-color: transparent;");

        // Локализация
        title.setText(LocalizationManager.getLocaleString("tags_title", "Редактор тегов"));
        save.setText(LocalizationManager.getLocaleString("tags_save", "Сохранить"));
    }
}
