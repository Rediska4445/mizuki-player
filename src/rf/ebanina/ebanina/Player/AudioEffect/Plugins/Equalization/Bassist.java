package rf.ebanina.ebanina.Player.AudioEffect.Plugins.Equalization;

import javafx.stage.Stage;

import rf.ebanina.ebanina.Player.AudioEffect.IAudioEffect;

import java.io.Serializable;
import java.util.Map;

public class Bassist implements IAudioEffect, Serializable {
    private static final long serialVersionUID = 1L;
    private float boostAmount;
    private boolean active = true;

    public Bassist(float boostAmount) {
        this.boostAmount = boostAmount;
    }

    public Bassist() {
        this.boostAmount = 0.5F;
    }

    public float getBoostAmount() {
        return boostAmount;
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
    public String getName() {
        return "Bassist";
    }

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
