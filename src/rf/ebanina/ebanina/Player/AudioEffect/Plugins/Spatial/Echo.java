package rf.ebanina.ebanina.Player.AudioEffect.Plugins.Spatial;

import javafx.stage.Stage;
import rf.ebanina.ebanina.Player.AudioEffect.IAudioEffect;

import java.util.Map;

public class Echo implements IAudioEffect {
    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setActive(boolean isActive) {

    }

    @Override
    public boolean isActive() {
        return false;
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
