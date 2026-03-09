package rf.ebanina.ebanina.Player.AudioEffect.Plugins;

import javafx.stage.Stage;
import rf.ebanina.ebanina.Player.AudioEffect.IAudioEffect;

import javax.sound.sampled.*;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class VolumeNormalizer implements IAudioEffect {
    private boolean active = false;
    private float targetVolume = 1.0f; // 1.0 = макс, 0.0 = тихо
    private final AtomicInteger micLevel = new AtomicInteger(0); // Уровень с микрофона (0-100)

    private ScheduledExecutorService executor;

    @Override
    public String getName() {
        return "Volume Normalizer (Microphone)";
    }

    @Override
    public void setActive(boolean isActive) {
        if (this.active == isActive) return;
        this.active = isActive;

        if (isActive) {
            startMicrophoneMonitoring();
        } else {
            stopMicrophoneMonitoring();
        }
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
        if (!active || in.length == 0) return in;

        int level = micLevel.get();

        float factor = 1.0f - (level / 100.0f) * 0.8f; // 1.0 → 0.2
        targetVolume = Math.max(0.1f, factor);

        for (int ch = 0; ch < in.length; ch++) {
            for (int i = 0; i < frames; i++) {
                in[ch][i] *= targetVolume;
            }
        }

        return in;
    }

    private void startMicrophoneMonitoring() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::readMicrophoneLevel, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void stopMicrophoneMonitoring() {
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void readMicrophoneLevel() {
        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                return;
            }

            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            byte[] buffer = new byte[1024];
            line.read(buffer, 0, buffer.length);

            int level = 0;
            for (byte b : buffer) {
                level += b * b;
            }
            level = (int) Math.sqrt(level / buffer.length);

            this.micLevel.set(Math.min(100, level / 10));

            line.stop();
            line.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, String> load() {
        return null;
    }
}


