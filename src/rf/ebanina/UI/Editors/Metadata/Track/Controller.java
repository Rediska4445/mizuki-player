package rf.ebanina.UI.Editors.Metadata.Track;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import me.API.Info;
import org.json.simple.parser.ParseException;
import rf.ebanina.File.Metadata.MetadataOfFile;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;

public class Controller implements
        Initializable
{
    public TextField command_field;
    public Button remove;
    public Button save;
    public TextArea lyrics;
    public VBox metadata;
    public TextField author;
    public TextField title;
    public Rectangle album_art;

    private Track track;

    @FXML
    protected VBox mainBox;

    private static final ExecutorService serv = Executors.newFixedThreadPool(1);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (track == null) {
            throw new IllegalStateException("Track не был установлен до вызова initialize()");
        }

        final Set<String> isActived = new HashSet<>();

        mainBox.getStylesheets().add(ResourceManager.Instance.loadStylesheet("scrollbar-fixed-width"));

        album_art.setFill(new ImagePattern(track.getAlbumArt(100)));
        title.setText(track.getTitle());
        author.setText(track.getArtist());
        lyrics.setText(MetadataOfFile.iMetadataOfFiles.getMetadataValue(track.getPath(), "lyrics"));

        command_field.setPromptText(getLocaleString("metadata_search", "Search"));
        save.setText(getLocaleString("metadata_save", "Save"));
        remove.setText(getLocaleString("metadata_remove", "Remove"));

        Color mainColor = ColorProcessor.core.getMainClr();
        String hexColor = ColorProcessor.core.toHex(mainColor);

        title.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-prompt-text-fill: #AAAAAA; -jfx-unfocus-color: " + hexColor + "; -jfx-focus-color: " + hexColor + ";");
        author.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-prompt-text-fill: #AAAAAA; -jfx-unfocus-color: " + hexColor + "; -jfx-focus-color: " + hexColor + ";");
        command_field.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-prompt-text-fill: #AAAAAA; -jfx-unfocus-color: " + hexColor + "; -jfx-focus-color: " + hexColor + ";");
        lyrics.setStyle("-fx-background-color: #333333; -fx-control-inner-background: #333333; -fx-text-fill: " + hexColor + "; -fx-prompt-text-fill: #AAAAAA;");

        save.setTextFill(mainColor);
        remove.setTextFill(mainColor);

        metadata.setStyle("-fx-background-color: #2D2D2D;");

        title.setOnKeyTyped(e -> isActived.add("title"));
        author.setOnKeyTyped(e -> isActived.add("author"));
        lyrics.setOnKeyTyped(e -> isActived.add("lyrics"));

        save.setOnAction(e -> {
            if (isActived.contains("title"))
                MetadataOfFile.iMetadataOfFiles.setTitle(track.getPath(), title.getText());

            if (isActived.contains("author"))
                MetadataOfFile.iMetadataOfFiles.setArtist(track.getPath(), author.getText());

            if (isActived.contains("lyrics"))
                MetadataOfFile.iMetadataOfFiles.setMetadataValue(track.getPath(), "lyrics", lyrics.getText());

            MetadataOfFile.iMetadataOfFiles.setArt(track.getPath(), SwingFXUtils.fromFXImage(((ImagePattern) album_art.getFill()).getImage(), null));
        });

        List<Map.Entry<String, String>> metadata = MetadataOfFile.iMetadataOfFiles.getAllMetadata(track.getPath());

        if(metadata != null) {
            for (Map.Entry<String, String> data : metadata) {
                HBox val = new HBox();
                Label key = new Label(data.getKey());
                TextField value = new TextField(data.getValue());

                val.setStyle("-fx-background-color: transparent;");
                val.setSpacing(20);
                val.setAlignment(Pos.CENTER_LEFT);
                val.setPadding(new Insets(5, 5, 5, 5));

                key.setStyle("-fx-font-weight: bold; -fx-text-fill: " + hexColor + "; -fx-background-color: transparent;");
                value.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-prompt-text-fill: #AAAAAA; -jfx-unfocus-color: " + hexColor + "; -jfx-focus-color: " + hexColor + ";");

                val.getChildren().add(key);
                val.getChildren().add(value);

                this.metadata.getChildren().add(val);
            }
        }

        command_field.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                serv.submit(() -> {
                    try {
                        URL url;
                        String query = command_field.getText();

                        if (query.startsWith("@search")) {
                            String searchQuery = query.substring(8).trim();
                            url = new URL(Info.info.search(URLEncoder.encode(searchQuery, StandardCharsets.UTF_8)).getAwesomeAlbumArt().getUrl());
                        } else if (query.startsWith("@current")) {
                            url = new URL(Info.info.search(URLEncoder.encode(
                                    Root.currentArtist.getText() + " - " + Root.currentTrackName.getText(),
                                    StandardCharsets.UTF_8)).getAwesomeAlbumArt().getUrl());
                        } if (query.startsWith("@dat")) {
                            url = new URL(Info.info.search(URLEncoder.encode(
                                    track.viewName(),
                                    StandardCharsets.UTF_8)).getAwesomeAlbumArt().getUrl());
                        } else {
                            url = new URL(query);
                        }

                        album_art.setFill(new ImagePattern(new Image(url.toString())));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } catch (ParseException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
        });
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public Track getTrack() {
        return track;
    }
}
