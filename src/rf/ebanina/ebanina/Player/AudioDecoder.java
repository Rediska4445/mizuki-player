package rf.ebanina.ebanina.Player;

import javax.sound.sampled.AudioInputStream;
import java.io.File;

/**
 * Декодер аудио файлов для потокового воспроизведения.
 * <p>
 * Предоставляет унифицированный API для различных аудио форматов.
 * </p>
 *
 * @implNote
 * Реализации: {@code MP3Decoder}, {@code WAVDecoder}, {@code FLACDecoder}, etc.
 * @see javax.sound.sampled.AudioInputStream
 */
public interface AudioDecoder
{
    /**
     * Возвращает поддерживаемый MIME-тип/расширение формата.
     * <p>
     * <b>Примеры:</b> ".mp3", ".wav", ".flac"
     * </p>
     *
     * @return строковый идентификатор формата
     */
    String getFormat();
    /**
     * Вычисляет длительность аудио файла без полной загрузки.
     * <p>
     * <b>Оптимизация:</b> читает только заголовок/метаданные
     * </p>
     *
     * @param var1 аудио файл
     * @return длительность в секундах (double)
     */
    double computeAudioDuration(File var1);
    /**
     * Создает потоковый AudioInputStream для воспроизведения.
     * <p>
     * <b>Особенности:</b>
     * </p>
     * <ul>
     *   <li>Поддерживает декодирование сжатых форматов (MP3→PCM)</li>
     *   <li>Lazy loading: декодирует по мере чтения</li>
     *   <li>Не загружает весь файл в память</li>
     * </ul>
     *
     * @param var1 аудио файл
     * @return AudioInputStream для {@link javax.sound.sampled.SourceDataLine}
     */
    AudioInputStream createStreaming(File var1);
}