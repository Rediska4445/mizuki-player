package test;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class SwingMemoryAudioPlayerWithSlider extends JFrame {
    private JButton playButton, stopButton;
    private JSlider seekSlider;
    private byte[] audioData;
    private AudioFormat format;
    private SourceDataLine line;
    private volatile boolean playing = false;
    private volatile int position = 0; // текущая позиция в байтах
    private boolean userSeeking = false;

    public SwingMemoryAudioPlayerWithSlider(String filePath) {
        super("Memory Audio Player with Slider");
        loadAudioFile(filePath);
        setupLine();
        initUI();
    }

    private void loadAudioFile(String filePath) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(filePath));
            format = ais.getFormat();
            audioData = ais.readAllBytes();
            ais.close();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading audio file: " + e.getMessage());
        }
    }

    private void setupLine() {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Audio line unavailable");
        }
    }

    private void initUI() {
        playButton = new JButton("Play");
        stopButton = new JButton("Stop");
        // Создаем JSlider на всю длительность файла
        int maxFrames = audioData.length / format.getFrameSize();
        seekSlider = new JSlider(0, maxFrames, 0);

        // Настройка слушателя для перемещения ползунка
        seekSlider.addChangeListener(e -> {
            if (seekSlider.getValueIsAdjusting()) {
                userSeeking = true;
            } else {
                if (userSeeking) {
                    int framePos = seekSlider.getValue();
                    seekTo(framePos);
                    userSeeking = false;
                }
            }
        });

        playButton.addActionListener(e -> startPlayback());
        stopButton.addActionListener(e -> stopPlayback());

        JPanel controlPanel = new JPanel();
        controlPanel.add(playButton);
        controlPanel.add(stopButton);

        this.setLayout(new BorderLayout());
        this.add(seekSlider, BorderLayout.CENTER);
        this.add(controlPanel, BorderLayout.SOUTH);

        setSize(600, 150);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private AudioInputStream createStreamFromMemory() {
        ByteArrayInputStream bais = new ByteArrayInputStream(audioData);

        try {
            AudioInputStream ais = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());
            ais.skip(position);
            return ais;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void startPlayback() {
        if (playing) return;
        playing = true;

        new Thread(() -> {
            try (AudioInputStream ais = createStreamFromMemory()) {
                line.start();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while (playing && (bytesRead = ais.read(buffer)) != -1) {
                    line.write(buffer, 0, bytesRead);
                    synchronized (this) {
                        position += bytesRead;
                        int framePos = position / format.getFrameSize();
                        SwingUtilities.invokeLater(() -> {
                            if (!seekSlider.getValueIsAdjusting()) {
                                seekSlider.setValue(framePos);
                            }
                        });
                    }
                }
                line.drain();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                line.stop();
                line.flush();
                playing = false;
            }
        }).start();
    }

    private void stopPlayback() {
        playing = false;
        line.stop();
        line.flush();
    }

    private void seekTo(int framePos) {
        int bytePos = framePos * format.getFrameSize();
        if (bytePos >= 0 && bytePos < audioData.length) {
            synchronized (this) {
                position = bytePos;
            }
            if (playing) {
                stopPlayback();
                startPlayback();
            }
        }
    }

    public static void main(String[] args) {
        String filePath = "C:\\Users\\2022\\Music\\wav - test\\Kordhell - Wig Split (online-audio-converter.com).wav"; // Укажите свой путь
        SwingUtilities.invokeLater(() -> {
            new SwingMemoryAudioPlayerWithSlider(filePath).setVisible(true);
        });
    }
}
