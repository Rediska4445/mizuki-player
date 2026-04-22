package rf.ebanina.ebanina.Player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.nio.file.Path;

public class Intro
        implements MediaReference
{
    protected String source;

    public Intro() {}

    public Intro(Path path) {
        this.source = path.toFile().getAbsolutePath();
    }

    public Intro(File file) {
        this.source = file.getAbsolutePath();
    }

    public Intro(String source) {
        this.source = source;
    }

    public File getFileSource() {
        return new File(source);
    }

    public Intro setFileSource(File source) {
        this.source = source.getAbsolutePath();
        return this;
    }

    public String getSource() {
        return source;
    }

    public Intro setSource(String source) {
        this.source = source;
        return this;
    }

    /**
     * Отдельный поток для проигрывания интро.
     * <p>
     * Загружает аудиофайл, проверяет формат (поддерживается только 16-bit PCM signed little-endian),
     * применяет fade-in и кроссфейд, затем проигрывает интро перед основным треком.
     * Автоматически освобождает ресурсы.
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * player.playIntro();
     * }</pre>
     * @see #playIntro()
     * @see Media#getIntroSoundFile()
     */
    private Thread introThread = new Thread(() -> {
        try (AudioInputStream introStream = AudioSystem.getAudioInputStream(new File(source))) {
            AudioFormat format = introStream.getFormat();

            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || format.getSampleSizeInBits() != 16 || format.isBigEndian()) {
                throw new UnsupportedOperationException("Требуется 16-bit PCM signed little endian аудио для интро");
            }

            SourceDataLine introLine = AudioSystem.getSourceDataLine(format);
            introLine.open(format);
            introLine.start();

            int frameSize = format.getFrameSize();
            int sampleRate = (int) format.getSampleRate();

            int fadeInMs = 1000;
            int fadeInFrames = (fadeInMs * sampleRate) / 1000;

            int crossfadeMs = 1000;
            int crossfadeFrames = (crossfadeMs * sampleRate) / 1000;

            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalFramesRead = 0;

            while ((bytesRead = introStream.read(buffer)) != -1) {
                int framesRead = bytesRead / frameSize;

                for (int frame = 0; frame < framesRead; frame++) {
                    double amp = 1.0;
                    int currentFrame = totalFramesRead + frame;
                    if (currentFrame < fadeInFrames) {
                        amp = (double) currentFrame / fadeInFrames;
                    }

                    for (int byteIndex = 0; byteIndex < frameSize; byteIndex += 2) {
                        int sampleIndex = frame * frameSize + byteIndex;

                        int low = buffer[sampleIndex] & 0xFF;
                        int high = buffer[sampleIndex + 1];
                        int sample = (high << 8) | low;

                        int newSample = (int) (sample * amp);

                        if (newSample > 32767) newSample = 32767;
                        if (newSample < -32768) newSample = -32768;

                        buffer[sampleIndex] = (byte) (newSample & 0xFF);
                        buffer[sampleIndex + 1] = (byte) ((newSample >> 8) & 0xFF);
                    }
                }

                introLine.write(buffer, 0, bytesRead);
                totalFramesRead += framesRead;

                if (totalFramesRead >= crossfadeFrames) {
                    break;
                }
            }

            while ((bytesRead = introStream.read(buffer)) != -1) {
                introLine.write(buffer, 0, bytesRead);
            }

            introLine.drain();
            introLine.stop();
            introLine.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }, "intro");

    public void play() {
        if(introThread.getState() != Thread.State.RUNNABLE) {
            introThread.start();
        }
    }

    public Thread getIntroThread() {
        return introThread;
    }

    protected Intro setIntroThread(Thread introThread) {
        this.introThread = introThread;
        return this;
    }

    @Override
    public String getPath() {
        return source;
    }

    @Override
    public boolean isNetty() {
        return false;
    }
}
