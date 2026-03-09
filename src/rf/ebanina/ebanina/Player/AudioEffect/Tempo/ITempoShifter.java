package rf.ebanina.ebanina.Player.AudioEffect.Tempo;

public interface ITempoShifter {
    float[][] applyTempoAndPitchCubic(float[][] input, int frames, int channels, float tempo);
}
