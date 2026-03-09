package rf.ebanina.UI.UI.Element.Slider;

import com.jfoenix.controls.JFXSlider;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javazoom.jl.decoder.Bitstream;
import rf.ebanina.ebanina.Player.AudioDecoder;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.ebanina.Player.MediaPlayer;
import rf.ebanina.ebanina.Player.Mp3PcmStream;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Resources.ResourceManager;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.io.*;

import static rf.ebanina.UI.Root.stage;

public class SoundSlider extends JFXSlider {
    private Pane sliderBackground;
    private ObjectProperty<Color> colorProperty;
    private Duration timeAnimation = Duration.millis(125);
    private Dimension size = new Dimension(0, 0);
    private final int boxWidth = 4;
    private final int arc = 6;
    private float[] samples;

    public SoundSlider() {
        this(0, 120, 0);
    }

    public SoundSlider(int min, int max, int val) {
        super(min, max, val);

        setShowTickMarks(false);
        setBlockIncrement(1.0);
        setMajorTickUnit(5.0);
        setMinorTickCount(4);
        setId("color-slider");
        getStylesheets().add(ResourceManager.Instance.loadStylesheet("slider-custom"));
        valueProperty().addListener((observableValue, number, t1) -> updateSlider());

        colorProperty = new SimpleObjectProperty<>();
        colorProperty.set(Color.BLACK);

        sliderBackground = new Pane();
        sliderBackground.setMouseTransparent(true);
        sliderBackground.layoutXProperty().bind(layoutXProperty());
        sliderBackground.layoutYProperty().bind(layoutYProperty());
    }

    public Pane getSliderBackground() {
        return sliderBackground;
    }

    public SoundSlider setSliderBackground(Pane sliderBackground) {
        this.sliderBackground = sliderBackground;
        return this;
    }

    public Interpolator interpolator = Interpolator.EASE_IN;

    public Interpolator getInterpolator() {
        return interpolator;
    }

    public SoundSlider setInterpolator(Interpolator interpolator) {
        this.interpolator = interpolator;
        return this;
    }

    public void updateSlider() {
        Platform.runLater(() -> {
            int size = sliderBackground.getChildren().size();
            double value = getValue();
            double max = getMax();

            if (value <= 0 || max <= 0 || size == 0) {
                return;
            }

            int activeCount = (int) ((value / max) * size);

            for (int i = activeCount; i < size; i++) {
                Rectangle rect = (Rectangle) sliderBackground.getChildren().get(i);

                if(stage.isFocused()) {
                    new Timeline(new KeyFrame(timeAnimation,
                            new KeyValue(rect.fillProperty(), Color.DARKGRAY, interpolator))).play();
                } else {
                    rect.setFill(Color.DARKGRAY);
                }
            }

            for (int i = 0; i < activeCount; i++) {
                Rectangle rect = (Rectangle) sliderBackground.getChildren().get(i);

                if(stage.isFocused()) {
                    new Timeline(new KeyFrame(timeAnimation,
                            new KeyValue(rect.fillProperty(), colorProperty.get(), interpolator))).play();
                } else {
                    rect.setFill(colorProperty.get());
                }
            }
        });
    }

    public void setSamples(float[] var) {
        samples = var;
    }

    public float[] getSamples() {
        return samples;
    }

    public Thread setupSliderBoxAsync() {
        return new Thread(this::initializeBox);
    }

    public void initializeBox() {
        Platform.runLater(() -> {
            sliderBackground.getChildren().clear();

            for (int i = 0; i < size.width / boxWidth; i++) {
                Rectangle g2d1 = new Rectangle();

                int x = i * boxWidth;

                g2d1.setX(x + 1);
                g2d1.setFill(Color.DARKGRAY);
                g2d1.setWidth(boxWidth - 1);
                g2d1.setHeight(size.height);
                g2d1.setArcHeight(arc);
                g2d1.setArcWidth(arc);

                sliderBackground.getChildren().add(g2d1);
            }
        });
    }

    public float[] readSamples(int frameLength, AudioInputStream in, byte[] buffer, int bytesPerSample, boolean isBigEndian, int channels) throws IOException {
        float[] samples = new float[frameLength];

        int bytesRead;
        int sampleIndex = 0;

        while ((bytesRead = in.read(buffer)) != -1 && sampleIndex < samples.length) {
            int offset = 0;

            while (offset < bytesRead && sampleIndex < samples.length) {
                int sum = 0;
                for (int c = 0; c < channels; c++) {
                    int sample;

                    if (bytesPerSample == 1) {
                        sample = (buffer[offset++] & 0xFF) << 8;
                    } else {
                        if (isBigEndian) {
                            sample = ((buffer[offset++] & 0xFF) << 8) | (buffer[offset++] & 0xFF);
                        } else {
                            sample = (buffer[offset++] & 0xFF) | ((buffer[offset++] & 0xFF) << 8);
                        }
                    }

                    if ((sample & 0x8000) != 0) {
                        sample |= 0xFFFF0000;
                    }

                    sum += sample;
                }

                samples[sampleIndex++] = sum / (float) channels;
            }
        }

        return samples;
    }

    private File file;

    public File getFile() {
        return file;
    }

    public float[] getSamples(File file) {
        if(getSamples() == null)
            initSamples(this.file = file);

        return getSamples();
    }

    public void clearSamples() {
        setSamples(null);
    }

    public void setupDefaultBox() {
        int sizeChildren = sliderBackground.getChildren().size();
        int expectedCount = size.width / boxWidth;
        int count = Math.min(sizeChildren, expectedCount);

        for (int i = 0; i < count; i++) {
            int x = i * boxWidth;
            Rectangle rect = (Rectangle) sliderBackground.getChildren().get(i);

            rect.setFill(Color.DARKGRAY);
            rect.setX(x + 1);
            rect.setY(10);
            rect.setWidth(boxWidth - 1);
            rect.setArcHeight(arc);
            rect.setArcWidth(arc);

            new Timeline(
                    new KeyFrame(timeAnimation,
                            new KeyValue(rect.heightProperty(), 5))).play();
        }
    }

    public void loadBox(float[] samples) {
        int numSubsets = size.width / boxWidth;

        Platform.runLater(() -> {
            int currentCount = sliderBackground.getChildren().size();
            if (currentCount < numSubsets) {
                for (int i = currentCount; i < numSubsets; i++) {
                    Rectangle rect = new Rectangle();
                    int x = i * boxWidth;
                    rect.setX(x + 1);
                    rect.setY(10);
                    rect.setWidth(boxWidth - 1);
                    rect.setHeight(5);
                    rect.setFill(Color.DARKGRAY);
                    rect.setArcHeight(arc);
                    rect.setArcWidth(arc);
                    sliderBackground.getChildren().add(rect);
                }

            } else if (currentCount > numSubsets) {
                sliderBackground.getChildren().remove(numSubsets, currentCount);
            }
        });

        int subsetLength = samples.length / numSubsets;
        float[] subsets = new float[numSubsets];
        int s = 0;
        for (int i = 0; i < numSubsets; i++) {
            int end = Math.min(s + subsetLength, samples.length);
            double sum = 0;

            for (int k = s; k < end; k++) {
                sum += Math.abs(samples[k]);
            }

            int length = end - s;
            subsets[i] = (length > 0) ? (float) (sum / length) : 0;
            s = end;
        }

        float maxVal = 0;
        for (float val : subsets) {
            if (val > maxVal) maxVal = val;
        }

        float normal = (maxVal > 0) ? 32768.0f / maxVal : 1.0f;

        Platform.runLater(() -> {
            for (int i = 0; i < subsets.length; i++) {
                float normalizedHeight = (subsets[i] * normal / 32768.0f) * (size.height / 2);
                int sample = (int) normalizedHeight;

                int posY = (size.height / 2) - sample;
                int negY = (size.height / 2) + sample;

                Rectangle rect = (Rectangle) sliderBackground.getChildren().get(i);
                rect.setFill(Color.DARKGRAY);

                new Timeline(
                        new KeyFrame(timeAnimation,
                                new KeyValue(rect.yProperty(), posY))).play();

                new Timeline(
                        new KeyFrame(timeAnimation,
                                new KeyValue(rect.heightProperty(), negY - posY + 2))).play();
            }
        });
    }

    public void initSamples(File file) {
        clearSamples();

        this.file = file;

        String fileName = file.getName().toLowerCase();
        AudioInputStream in = null;

        try (InputStream rawInput = new BufferedInputStream(new FileInputStream(file))) {
            AudioFormat format = null;
            long frameLength = 0;

            if (fileName.endsWith(MediaPlayer.AvailableFormat.WAV.getTitle())) {
                in = AudioSystem.getAudioInputStream(rawInput);
                format = in.getFormat();
                frameLength = in.getFrameLength();
            } else if (fileName.endsWith(MediaPlayer.AvailableFormat.MP3.getTitle())) {
                Mp3File mp3file = new Mp3File(file);

                long durationSeconds = mp3file.getLengthInSeconds();
                int sampleRate = mp3file.getSampleRate();
                int channels = (mp3file.getChannelMode().length() == 3) ? 1 : 2;

                format = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        sampleRate,
                        16,
                        channels,
                        channels * 2,
                        sampleRate,
                        false);

                frameLength = durationSeconds * sampleRate;

                Bitstream bitstream = new Bitstream(rawInput);

                Mp3PcmStream mp3PcmStream = new Mp3PcmStream(bitstream);

                in = new AudioInputStream(mp3PcmStream, format, frameLength);
            } else {
                for (AudioDecoder dec : MediaProcessor.mediaProcessor.mediaPlayer.getDecoders()) {
                    if (fileName.endsWith(dec.getFormat())) {
                        in = dec.createStreaming(file);

                        if (in != null) {
                            format = in.getFormat();
                            frameLength = in.getFrameLength();
                            break;
                        }
                    }
                }

                if (in == null) {
                    throw new UnsupportedAudioFileException("Unsupported audio format: " + fileName);
                }
            }

            if (format == null || format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                throw new UnsupportedAudioFileException("Audio encoding must be PCM_SIGNED");
            }

            boolean big = format.isBigEndian();
            int chans = format.getChannels();
            int bits = format.getSampleSizeInBits();
            int bytesPerSample = bits / 8;

            int bufferLength = chans * bytesPerSample * 8192;
            byte[] buf = new byte[bufferLength];

            samples = readSamples((int) frameLength, in, buf, bytesPerSample, big, chans);

            in.close();
        } catch (IOException | UnsupportedAudioFileException | InvalidDataException | UnsupportedTagException e) {
            throw new RuntimeException(e);
        }
    }

    public Runnable onLoadedSliderBackground;

    public void loadSliderBackground(File file) {
        initSamples(file);
        loadBox(getSamples());

        if(onLoadedSliderBackground != null) {
            onLoadedSliderBackground.run();
        }

        if(ConfigurationManager.instance.getBooleanItem("clear_samples", "true")) {
            clearSamples();
        }
    }

    public void setColor(Color colorProperty) {
        this.colorProperty.set(colorProperty);
    }

    public ObjectProperty<Color> getColorProperty() {
        return colorProperty;
    }

    public void setLayouts(double x, double y) {
        super.setLayoutX(x);
        super.setLayoutY(y);
    }

    public ObjectProperty<Color> colorPropertyProperty() {
        return colorProperty;
    }

    public Duration getTimeAnimation() {
        return timeAnimation;
    }

    public SoundSlider setTimeAnimation(Duration timeAnimation) {
        this.timeAnimation = timeAnimation;
        return this;
    }

    public Dimension getSize() {
        return size;
    }

    public SoundSlider setSize(Dimension size) {
        this.size = size;
        return this;
    }

    public int getBoxWidth() {
        return boxWidth;
    }

    public int getArc() {
        return arc;
    }
}
