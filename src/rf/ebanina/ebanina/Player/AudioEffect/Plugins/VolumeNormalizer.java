package rf.ebanina.ebanina.Player.AudioEffect.Plugins;

import javafx.stage.Stage;
import rf.ebanina.ebanina.Player.AudioEffect.IAudioEffect;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Плагин-нормализатор громкости для аудиоплеера.
 * <p>
 * Автоматически стабилизирует уровень громкости воспроизводимого аудио
 * в зависимости от текущего уровня громкости микрофона. При высокой
 * громкости микрофона (громкий голос/шум) автоматически снижает
 * громкость плеера, при низкой - повышает до целевого уровня.
 * </p>
 *
 * <h3>Принцип работы:</h3>
 * <ul>
 *   <li>Мониторит уровень сигнала микрофона каждые 50мс</li>
 *   <li>Вычисляет RMS уровень аудио с микрофона</li>
 *   <li>Применяет инверсную зависимость: высокий микрофон → низкая громкость плеера</li>
 *   <li>Громкость плеера: 100% (тихо в микрофоне) → 20% (громко в микрофоне)</li>
 * </ul>
 *
 * <h3>Формула расчета:</h3>
 * <pre>{@code
 * factor = 1.0 - (micLevel/100) * 0.8
 * targetVolume = max(0.1, factor)
 * }</pre>
 *
 * <p><strong>Применение:</strong> Идеально для стримеров, подкастеров,
 * конференций - автоматическое выравнивание микрофона и музыки.</p>
 *
 * @author Ebanina
 * @version 1.0
 * @since 1.0
 */
public class VolumeNormalizer
        implements IAudioEffect
{
    /** Состояние активности плагина */
    private boolean active = false;
    /** Целевая громкость воспроизведения (0.1-1.0) */
    private float targetVolume = 1.0f;
    /** Текущий уровень сигнала микрофона (0-100) */
    private final AtomicInteger micLevel = new AtomicInteger(0);

    private ScheduledExecutorService executor;

    /**
     * Возвращает название плагина для UI.
     *
     * @return название "Volume Normalizer (Microphone)"
     */
    @Override
    public String getName() {
        return "Volume Normalizer (Microphone)";
    }

    /**
     * Активирует/деактивирует плагин.
     *
     * @param isActive true - запуск мониторинга микрофона,
     *                 false - остановка мониторинга
     */
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
    /**
     * Проверяет активность плагина.
     *
     * @return true если плагин активен и мониторит микрофон
     */
    @Override
    public boolean isActive() {
        return active;
    }
    /**
     * Открывает редактор настроек плагина.
     * <p>Текущая реализация не требует GUI настроек.</p>
     *
     * @param parent родительское окно JavaFX
     */
    @Override
    public void openEditor(Stage parent) {

    }
    /**
     * Обрабатывает аудио буфер, применяя нормализацию громкости.
     *
     * @param in входной стерео/мультиканальный буфер
     * @param frames количество сэмплов в буфере
     * @return обработанный буфер с примененной нормализацией
     */
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
    /**
     * Запускает фоновый мониторинг микрофона.
     * Планирует чтение каждые 50мс в отдельном потоке.
     */
    private void startMicrophoneMonitoring() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::readMicrophoneLevel, 0, 50, TimeUnit.MILLISECONDS);
    }
    /**
     * Останавливает мониторинг микрофона.
     * Безопасно завершает планировщик с таймаутом 1с.
     */
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
    /**
     * Читает текущий уровень сигнала с микрофона.
     * <p>Алгоритм:
     * <ol>
     *   <li>Открывает микрофон (44.1kHz, 16bit, моно)</li>
     *   <li>Читает 1024 байта</li>
     *   <li>Вычисляет RMS: sqrt(∑(sample²)/N)</li>
     *   <li>Масштабирует в 0-100 и сохраняет</li>
     * </ol></p>
     */
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

    /**
     * Загружает настройки плагина из конфигурации.
     *
     * @return Map с настройками или null (нет сохраненных настроек)
     */
    @Override
    public Map<String, String> load() {
        return null;
    }
}


