package rf.ebanina.ebanina.Player.AudioEffect.Plugins.Compression;

import javafx.stage.Stage;
import rf.ebanina.ebanina.Player.AudioEffect.IAudioEffect;

import java.util.Map;

public class Clipper implements IAudioEffect {
    private boolean active = true;

    @Override
    public String getName() {
        return "Clipper";
    }

    @Override
    public void setActive(boolean isActive) {
        this.active = isActive;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void openEditor(Stage parent) {

    }

    @Override
    public float[][] process(float[][] in, int frames) {
        return in;
    }

    @Override
    public Map<String, String> load() {
        return null;
    }
}