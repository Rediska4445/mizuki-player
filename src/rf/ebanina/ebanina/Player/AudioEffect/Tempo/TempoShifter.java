package rf.ebanina.ebanina.Player.AudioEffect.Tempo;

public class TempoShifter implements ITempoShifter {
    @Override
    public float[][] applyTempoAndPitchCubic(float[][] input, int frames, int channels, float tempo) {
        int newFrames = Math.round(frames / tempo);
        float[][] output = new float[channels][newFrames];

        for (int ch = 0; ch < channels; ch++) {
            for (int i = 0; i < newFrames; i++) {
                float srcIndex = i * tempo;
                int i0 = (int) Math.floor(srcIndex) - 1;
                float t = srcIndex - (i0 + 1);

                float s0 = getSampleSafe(input[ch], i0, frames);
                float s1 = getSampleSafe(input[ch], i0 + 1, frames);
                float s2 = getSampleSafe(input[ch], i0 + 2, frames);
                float s3 = getSampleSafe(input[ch], i0 + 3, frames);

                output[ch][i] = cubicInterpolate(s0, s1, s2, s3, t);
            }
        }

        return output;
    }

    private float getSampleSafe(float[] data, int index, int length) {
        if (data == null || data.length == 0)
            return 0f;
        if (index < 0)
            return data[0];
        int maxIndex = Math.min(length, data.length) - 1;
        if (index > maxIndex)
            return data[maxIndex];
        return data[index];
    }

    private float cubicInterpolate(float y0, float y1, float y2, float y3, float t) {
        float a0 = y3 - y2 - y0 + y1;
        float a1 = y0 - y1 - a0;
        float a2 = y2 - y0;
        float a3 = y1;

        return (a0 * t * t * t) + (a1 * t * t) + (a2 * t) + a3;
    }
}
