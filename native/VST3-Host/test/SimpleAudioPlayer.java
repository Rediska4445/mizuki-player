package test;

import rf.vst3;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleAudioPlayer extends JFrame {
    private JButton openFileButton, playButton, stopButton, reOpenVST3Button, openVST3Button, loadPluginButton;
    private JComboBox<String> pluginSelector;
    private volatile boolean playing = false;
    private Thread playbackThread;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private final List<vst3> pluginChain = new ArrayList<>();
    private File selectedFile = null;

    public SimpleAudioPlayer() {
        super("Аудиоплеер с цепочкой VST3 плагинов");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(650, 180);
        setLayout(new FlowLayout());

        openFileButton = new JButton("Открыть WAV файл");
        playButton = new JButton("Воспроизвести");
        stopButton = new JButton("Стоп");
        openVST3Button = new JButton("Открыть VST3 редактор");
        reOpenVST3Button = new JButton("Переоткрыть VST3 редактор");
        loadPluginButton = new JButton("Загрузить плагин");
        pluginSelector = new JComboBox<>();

        add(openFileButton);
        add(playButton);
        add(stopButton);
        add(new JLabel("Плагин в цепочке:"));
        add(pluginSelector);
        add(openVST3Button);
        add(reOpenVST3Button);
        add(loadPluginButton);

        openFileButton.addActionListener(e -> openAudioFile());
        playButton.addActionListener(e -> startPlayback());
        stopButton.addActionListener(e -> stopPlayback());
        loadPluginButton.addActionListener(e -> loadNewPlugin());
        openVST3Button.addActionListener(e -> openVstEditor());
        reOpenVST3Button.addActionListener(e -> reOpenVstEditor());
        pluginSelector.addActionListener(e -> onPluginSelected());

        loadDefaultPlugins();

        // Устанавливаем WAV файл по умолчанию
        selectedFile = new File("C:\\Users\\2022\\Music\\wav - test\\Kordhell - Wig Split (online-audio-converter.com).wav");

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadDefaultPlugins() {
        String[] defaultPluginPaths = new String[] {
                "C:\\Program Files\\Common Files\\VST3\\BABY Audio\\Crystalline.vst3"
        };

        for (String path : defaultPluginPaths) {
            vst3 plugin = new vst3();
            plugin.setLoggingEnable(true);

            plugin.setOnInitialize(() -> {
                System.out.println("PluginWrapper initialized: ");

                System.out.println("ESFWEF: ");
                System.out.println(plugin.getParameterCount());
                System.out.println("ewfEF");

//                for(int i = 1; i < plugin.getParameterCount(); i++) {
//                    System.out.println(plugin.getParameterValue(i));
//                    plugin.setParameterValue(i, 1);
//                }

                SwingUtilities.invokeLater(() -> {
                    if (!pluginChain.contains(plugin)) {
                        pluginChain.add(plugin);
                        pluginSelector.addItem(new File(path).getName());

                        if (pluginChain.size() == 1) {
                            pluginSelector.setSelectedIndex(0);
                            updateButtons();
                        }
                    }
                });

                System.out.println(plugin.getVendor() + "\n" + plugin.getPluginName() + "\n" + plugin.getSdkVersion() + "\n" + plugin.getCategory());
            });

            try {
                if(plugin.asyncInit(new File(path), 44100, BLOCK_SIZE * 2, 0, true).get()) {
                    System.out.println("PluginWrapper is VST3");
                } else {
                    System.out.println("PluginWrapper is not VST3");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void onPluginSelected() {
        updateButtons();
    }

    private void openAudioFile() {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            updateButtons();
        }
    }

    static final int BLOCK_SIZE = 1024;

    private void startPlayback() {
        vst3 vst = getCurrentPlugin();

        System.out.println(vst);

        playing = true;

        playbackThread = new Thread(() -> {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(selectedFile)) {
                AudioFormat baseFormat = ais.getFormat();

                AudioFormat floatFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_FLOAT,
                        baseFormat.getSampleRate(),
                        32,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 4,
                        baseFormat.getSampleRate(),
                        false);

                try (AudioInputStream din = AudioSystem.getAudioInputStream(floatFormat, ais)) {
                    int maxInChannels = 0;
                    int maxOutChannels = 0;

                    for (vst3 p : pluginChain) {
                        int inCh = 0;
                        for (int i = 0; i < p.getNumInputs(); i++)
                            inCh += p.getNumChannelsForInputBus(i);

                        maxInChannels = Math.max(maxInChannels, inCh);

                        int outCh = 0;
                        for (int i = 0; i < p.getNumOutputs(); i++)
                            outCh += p.getNumChannelsForOutputBus(i);

                        maxOutChannels = Math.max(maxOutChannels, outCh);
                    }

                    AudioFormat outFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            floatFormat.getSampleRate(),
                            16,
                            maxOutChannels,
                            maxOutChannels * 2,
                            floatFormat.getSampleRate(),
                            false);

                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, outFormat);

                    try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                        int bufferSize = BLOCK_SIZE * maxOutChannels * 2 * 4;
                        line.open(outFormat, bufferSize);
                        line.start();

                        byte[] buffer = new byte[BLOCK_SIZE * baseFormat.getChannels() * 4];
                        byte[] outBufferBytes = new byte[BLOCK_SIZE * maxOutChannels * 2];

                        long lastTime = System.nanoTime();

                        int framesRead;
                        float[][] inBuffers = new float[maxInChannels][BLOCK_SIZE];
                        float[][] outBuffers = new float[maxOutChannels][BLOCK_SIZE];

                        while (playing && (framesRead = din.read(buffer, 0, buffer.length)) != -1) {
                            framesRead /= (4 * baseFormat.getChannels());

                            for (int f = 0; f < framesRead; f++) {
                                for (int c = 0; c < baseFormat.getChannels(); c++) {
                                    int baseIndex = (f * baseFormat.getChannels() + c) * 4;
                                    int asInt = ((buffer[baseIndex] & 0xFF)
                                            | ((buffer[baseIndex + 1] & 0xFF) << 8)
                                            | ((buffer[baseIndex + 2] & 0xFF) << 16)
                                            | ((buffer[baseIndex + 3] & 0xFF) << 24));
                                    if (c < maxInChannels) {
                                        inBuffers[c][f] = Float.intBitsToFloat(asInt);
                                    }
                                }
                            }

                            for (int i = 0; i < pluginChain.size(); i++) {
                                vst3 plugin = pluginChain.get(i);

                                int pluginInCh = 0;
                                for (int b = 0; b < plugin.getNumInputs(); b++) {
                                    pluginInCh += plugin.getNumChannelsForInputBus(b);
                                }
                                int pluginOutCh = 0;
                                for (int b = 0; b < plugin.getNumOutputs(); b++) {
                                    pluginOutCh += plugin.getNumChannelsForOutputBus(b);
                                }

                                float[][] tmpIn = new float[pluginInCh][framesRead];
                                float[][] tmpOut = new float[pluginOutCh][framesRead];

                                for (int ch = 0; ch < pluginInCh; ch++) {
                                    System.arraycopy(inBuffers[ch], 0, tmpIn[ch], 0, framesRead);
                                }

                                plugin.process(tmpIn, tmpOut, framesRead);

                                for (int ch = 0; ch < pluginOutCh; ch++) {
                                    System.arraycopy(tmpOut[ch], 0, outBuffers[ch], 0, framesRead);
                                }

                                if (i < pluginChain.size() - 1) {
                                    for (int ch = 0; ch < pluginOutCh; ch++) {
                                        System.arraycopy(outBuffers[ch], 0, inBuffers[ch], 0, framesRead);
                                    }
                                }
                            }

                            for (int c = 0; c < maxOutChannels; c++) {
                                for (int f = 0; f < framesRead; f++) {
                                    if (outBuffers[c][f] > 1.0f) outBuffers[c][f] = 1.0f;
                                    if (outBuffers[c][f] < -1.0f) outBuffers[c][f] = -1.0f;
                                }
                            }

                            Arrays.fill(outBufferBytes, (byte) 0);
                            for (int f = 0; f < framesRead; f++) {
                                for (int c = 0; c < maxOutChannels; c++) {
                                    int sample16 = (int) (outBuffers[c][f] * 32767.0f);
                                    sample16 = Math.max(-32768, Math.min(32767, sample16));
                                    int baseIndex = (f * maxOutChannels + c) * 2;
                                    outBufferBytes[baseIndex] = (byte) (sample16 & 0xFF);
                                    outBufferBytes[baseIndex + 1] = (byte) ((sample16 >> 8) & 0xFF);
                                }
                            }

                            line.write(outBufferBytes, 0, framesRead * maxOutChannels * 2);

                            long currentTime = System.nanoTime();
                            double elapsedMs = (currentTime - lastTime) / 1_000_000.0;
                            lastTime = currentTime;
                        }

                        line.drain();
                        line.stop();
                    }
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Ошибка воспроизведения: " + ex.getMessage()));
            }

            SwingUtilities.invokeLater(() -> {
                playButton.setEnabled(true);
                stopButton.setEnabled(false);
            });

            playing = false;
        });

        playbackThread.start();
    }

    private void stopPlayback() {
        playing = false;
    }

    private void loadNewPlugin() {
        JFileChooser pluginChooser = new JFileChooser();
        pluginChooser.setDialogTitle("Выберите файл плагина VST3");

        int res = pluginChooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File pluginFile = pluginChooser.getSelectedFile();

            vst3 plugin = new vst3();
            plugin.setLoggingEnable(false);
            plugin.setOnInitialize(() -> {
                System.out.println("PluginWrapper initialized: ");
                System.out.println("ESFWEF: " + plugin.getParameterValue(1));

                SwingUtilities.invokeLater(() -> {
                    if (!pluginChain.contains(plugin)) {
                        pluginChain.add(plugin);
                        pluginSelector.addItem(pluginFile.getName());
                        updateButtons();
                    }
                });
            });

            plugin.asyncInit(pluginFile, 44100, 8192, 0, true);
        }
    }

    private void openVstEditor() {
        vst3 vst = getCurrentPlugin();

        System.out.println("Должен быть вызван: " + vst + "\n" + pluginSelector.getSelectedItem() + "\nНо вызывается:\n");

        if(vst != null)
            vst.asyncCreateView();
    }

    private void reOpenVstEditor() {
        SwingUtilities.invokeLater(() -> {
            vst3 vst = getCurrentPlugin();
            if (vst != null)
                vst.asyncReCreateView();
        });
    }

    private vst3 getCurrentPlugin() {
        int idx = pluginSelector.getSelectedIndex();
        if (idx < 0 || idx >= pluginChain.size())
            return null;

        return pluginChain.get(idx);
    }

    private void updateButtons() {
        boolean hasPlugin = !pluginChain.isEmpty();
        playButton.setEnabled(hasPlugin && selectedFile != null);
        openVST3Button.setEnabled(hasPlugin);
        reOpenVST3Button.setEnabled(hasPlugin);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimpleAudioPlayer::new);
    }
}
