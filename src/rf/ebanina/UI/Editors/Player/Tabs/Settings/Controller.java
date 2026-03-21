package rf.ebanina.UI.Editors.Player.Tabs.Settings;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.File.Metadata.MetadataOfFile;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.net.URI;
import java.nio.file.Paths;

import static rf.ebanina.UI.Root.*;

public class Controller {

    public Tab tab;
    public Label pitch;
    public Label volume_label1;
    public Label pan_text;
    public Label tempo;
    @FXML private Slider tempoSlider;
    @FXML private Slider pitchSlider;
    @FXML private Slider volume_slider;
    @FXML private Slider pan_slider;
    @FXML private Label tempoLabel;
    @FXML private Label pitchLabel;
    @FXML private Label volume_label;
    @FXML private Label pan_text1;
    @FXML private Pane panVolumeControlPad;
    @FXML private Pane pitchTempoControlPad;

    @FXML
    public void initialize() {
        tab.setText(LocalizationManager.getLocaleString("vst_editor_tab_parameters", "Settings"));

        // Инициализация значений
        tempoSlider.setValue(MediaProcessor.mediaProcessor.mediaPlayer.getTempo());
        volume_slider.setValue(MediaProcessor.mediaProcessor.mediaPlayer.getVolume());
        pan_slider.setValue(MediaProcessor.mediaProcessor.mediaPlayer.getPan());

        tempoLabel.setText(String.format("%.2f", tempoSlider.getValue()));
        pitchLabel.setText(String.format("%.2f", pitchSlider.getValue()));

        pitch.setText(LocalizationManager.getLocaleString("vst_editor_pitch", "Pitch"));
        tempo.setText(LocalizationManager.getLocaleString("vst_editor_tempo", "Tempo"));
        pan_text.setText(LocalizationManager.getLocaleString("vst_editor_pan", "Pan"));
        volume_label1.setText(LocalizationManager.getLocaleString("vst_editor_volume", "Volume"));

        applyDynamicStyles();

        setupListeners();
    }

    private void applyDynamicStyles() {
        Color clr = ColorProcessor.core.getMainClr();
        String hex = String.format("#%02X%02X%02X",
                (int)(clr.getRed()*255), (int)(clr.getGreen()*255), (int)(clr.getBlue()*255));

        String headerStyle = "-fx-text-fill: #E0E0E0; -fx-font-weight: bold;";
        pitch.setStyle(headerStyle);
        tempo.setStyle(headerStyle);
        pan_text.setStyle(headerStyle);
        volume_label1.setStyle(headerStyle);

        String valueStyle = "-fx-text-fill: " + hex + "; -fx-font-weight: 700;";
        tempoLabel.setStyle(valueStyle);
        pitchLabel.setStyle(valueStyle);
        volume_label.setStyle(valueStyle);
        pan_text1.setStyle(valueStyle);

        String padStyle = "-fx-background-color: rgba(255,255,255,0.03); " +
                "-fx-border-color: " + hex + "; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1;";
        pitchTempoControlPad.setStyle(padStyle);
        panVolumeControlPad.setStyle(padStyle);

        String fullSliderStyle =
                "-fx-accent: " + hex + "; " +
                        "-fx-control-inner-background: #333333;";

        Slider[] sliders = {tempoSlider, pitchSlider, volume_slider, pan_slider};
        for (Slider s : sliders) {
            s.setStyle(fullSliderStyle);
            s.applyCss();
            javafx.scene.Node thumb = s.lookup(".thumb");
            if (thumb != null) {
                thumb.setStyle("-fx-background-color: " + hex + ";");
            }
        }
    }

    private void setupListeners() {
        tempoSlider.valueProperty().addListener((obs, oldV, newV) -> {
            float val = (float) (Math.round(newV.doubleValue() * 100) / 100.0);
            tempoLabel.setText(String.format("%.2f", val));
            if (MediaProcessor.mediaProcessor.mediaPlayer != null) {
                MediaProcessor.mediaProcessor.globalMap.put("tempo", val, float.class);
                MediaProcessor.mediaProcessor.mediaPlayer.setTempo(val);

                soundSlider.setMax(MediaProcessor.mediaProcessor.mediaPlayer.recalculateOverDuration().toSeconds());
                endTime.setText(Track.getFormattedTotalDuration((float) soundSlider.getMax()));

                if(ConfigurationManager.instance.getBooleanItem("is_hue_change", "false")) {
                    ColorProcessor.core.scaleHue(val);
                    // Обновляем стили при изменении Hue, если это предусмотрено логикой
                    applyDynamicStyles();

                    artProcessor.setImage(MetadataOfFile.iMetadataOfFiles.getArt(new Track(Paths.get(URI.create(MediaProcessor.mediaProcessor.mediaPlayer.getMedia().getSource())).toString()), ColorProcessor.size, ColorProcessor.size, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth));
                    artProcessor.initColor(art.getImage());
                }
            }
        });

        pitchSlider.valueProperty().addListener((obs, oldV, newV) -> {
            float val = (float) (Math.round(newV.doubleValue() * 100) / 100.0);
            pitchLabel.setText(String.format("%.2f", val));
            if (MediaProcessor.mediaProcessor.mediaPlayer != null) {
                MediaProcessor.mediaProcessor.globalMap.put("pitch", val, float.class);
            }
        });

        volume_slider.valueProperty().addListener((o, old, newV) -> {
            double val = Math.round(newV.doubleValue() * 100) / 100.0;
            volume_label.setText(String.format("%.2f", val));
            if (MediaProcessor.mediaProcessor.mediaPlayer != null) {
                MediaProcessor.mediaProcessor.mediaPlayer.setVolume(val);
                MediaProcessor.mediaProcessor.globalMap.put("volume", val, double.class);
            }
        });

        pan_slider.valueProperty().addListener((obs, oldV, newV) -> {
            double val = Math.round(newV.doubleValue() * 100) / 100.0;
            pan_text1.setText(String.format("%.2f", val));
            if (MediaProcessor.mediaProcessor.mediaPlayer != null) {
                MediaProcessor.mediaProcessor.mediaPlayer.setPan((float) val);
                MediaProcessor.mediaProcessor.globalMap.put("pan", (float) val, float.class);
            }
        });
    }
}
