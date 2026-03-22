package rf.ebanina.ebanina.Player.AudioEffect.Tempo;

import rf.ebanina.File.Resources.ResourceManager;

import java.io.File;

public class NativeTempoShifter {
    public static final String TEMPO_SHIFTER_DLL = ResourceManager.BIN_LIBRARIES_PATH +
            File.separator + "tempoShifter.dll";

    static {
        System.load(TEMPO_SHIFTER_DLL);
    }

    public native int applyTempoNative(float[][] input, int frames, int channels, float tempo, float[][] output);

    public float[][] applyTempo(float[][] input, int frames, int channels, float tempo) {
        int newFrames = Math.round(frames / tempo);
        float[][] output = new float[channels][newFrames];

        applyTempoNative(input, frames, channels, tempo, output);

        return output;
    }
}