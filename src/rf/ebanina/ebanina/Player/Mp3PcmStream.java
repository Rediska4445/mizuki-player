package rf.ebanina.ebanina.Player;

import javazoom.jl.decoder.*;

import java.io.IOException;
import java.io.InputStream;

/**
 * <h1>Mp3PcmStream</h1>
 * Поток ввода, преобразующий сжатые MP3-данные в несжатый PCM-аудиопоток.
 * <p>
 * Этот класс является адаптером между битовым потоком MP3 ({@link Bitstream}) и
 * стандартным Java-потоком байтов ({@link InputStream}). Он декодирует MP3-фреймы
 * в реальном времени с помощью библиотеки odede>javazoom.jl.decoder</code> и
 * предоставляет результат как последовательность байтов в формате PCM.
 * </p>
 * <p>
 * Ключевые особенности:
 * <ul>
 *   <li><b>Потоковая декодировка</b> — MP3-данные обрабатываются по фреймам, что позволяет
 *       работать с большими файлами и сетевыми потоками без загрузки в память целиком.</li>
 *   <li><b>Формат PCM</b> — выходной поток содержит 16-битные моно/стерео сэмплы в little-endian,
 *       совместимые с большинством аудио-API Java.</li>
 *   <li><b>Интеграция с Java Sound</b> — полученный поток можно напрямую передать в
 *       {@link javax.sound.sampled.SourceDataLine} для воспроизведения.</li>
 * </ul>
 * </p>
 * <p>
 * Класс не является потокобезопасным. Все операции должны выполняться из одного потока.
 * При достижении конца потока MP3, методы {@link #read()} и {@link #read(byte[], int, int)}
 * возвращают odede>-1</code>, как и положено по контракту {@link InputStream}.
 * </p>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * try (FileInputStream fis = new FileInputStream("song.mp3");
 *      Bitstream bitstream = new Bitstream(fis);
 *      Mp3PcmStream pcmStream = new Mp3PcmStream(bitstream);
 *      AudioInputStream audioStream = new AudioInputStream(pcmStream, format, AudioSystem.NOT_SPECIFIED)) {
 *
 *     AudioPlayer.player.start(audioStream);
 * } catch (IOException e) {
 *     e.printStackTrace();
 * }
 * }</pre>
 *
 * <h3>Особенности</h3>
 * <ul>
 *   <li>Поддерживает только MP3-фреймы, декодируемые библиотекой odede>javazoom</code>.</li>
 *   <li>Не поддерживает метаданные (ID3 теги) — они должны быть пропущены перед созданием {@link Bitstream}.</li>
 *   <li>Не реализует методы odede>mark()</code> и <code>reset()</code> — повторное чтение невозможно.</li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 0.1.3.0
 * @see Bitstream
 * @see Decoder
 * @see InputStream
 * @see <a href="https://github.com/umjammer/jlayer">JLayer (javazoom) Project</a>
 */
public class Mp3PcmStream extends InputStream {
    /**
     * Битовый поток, содержащий сжатые MP3-данные.
     * <p>
     * Источник, из которого читаются MP3-фреймы с помощью {@link Bitstream#readFrame()}.
     * Должен быть корректно инициализирован и не содержать ID3-тегов в начале.
     * </p>
     * <p>
     * Объект передаётся извне и не закрывается в этом классе. Закрытие остаётся
     * на ответственности вызывающего кода.
     * </p>
     *
     * @see #Mp3PcmStream(Bitstream)
     * @see Bitstream#readFrame()
     * @since 0.1.3.0
     */
    private final Bitstream bitstream;
    /**
     * Декодер MP3, преобразующий фреймы в PCM-сэмплы.
     * <p>
     * Использует библиотеку <code>javazoom.jl.decoder.Decoder</code> для выполнения
     * основной работы по декодированию. Метод {@link Decoder#decodeFrame(Header, Bitstream)}
     * возвращает объект {@link SampleBuffer} с декодированными данными.
     * </p>
     * <p>
     * Экземпляр создаётся при инициализации и используется для всех операций чтения.
     * </p>
     *
     * @see Decoder#decodeFrame(Header, Bitstream)
     * @see SampleBuffer
     * @since 0.1.3.0
     */
    private final Decoder decoder = new Decoder();

    /**
     * Буфер с декодированными PCM-данными текущего фрейма.
     * <p>
     * Содержит байты в формате 16-bit little-endian. Размер буфера зависит от
     * количества сэмплов в MP3-фрейме (обычно 1152 для MPEG-1 Layer III).
     * </p>
     * <p>
     * Когда буфер пуст (или ещё не заполнен), вызывается декодировка следующего фрейма.
     * </p>
     *
     * @see #read()
     * @see #read(byte[], int, int)
     * @since 0.1.3.0
     */
    private byte[] currentFrameBytes;
    /**
     * Текущая позиция чтения в буфере {@link #currentFrameBytes}.
     * <p>
     * Указывает на следующий байт, который будет возвращён методом {@link #read()}.
     * Когда <code>framePos</code> достигает длины буфера, загружается следующий фрейм.
     * </p>
     *
     * @see #read()
     * @since 0.1.3.0
     */
    private int framePos = 0;
    /**
     * Флаг, указывающий, что конец MP3-потока достигнут.
     * <p>
     * Устанавливается в <code>true</code>, когда {@link Bitstream#readFrame()} возвращает <code>null</code>.
     * После этого все последующие вызовы {@link #read()} возвращают <code>-1</code>.
     * </p>
     *
     * @see #read()
     * @since 0.1.3.0
     */
    private boolean endOfStream = false;

    /**
     * Создаёт новый поток PCM из заданного битового потока MP3.
     * <p>
     * Принимает уже инициализированный {@link Bitstream}, который должен указывать
     * на начало MP3-данных (после ID3-тегов).
     * </p>
     * <p>
     * Декодер и внутренние буферы инициализируются при первом вызове {@link #read()}.
     * </p>
     *
     * @param bitstream битовый поток с MP3-данными; не должен быть <code>null</code>
     * @throws NullPointerException если <code>bitstream</code> равен <code>null</code>
     * @see Bitstream
     * @since 0.1.3.0
     */
    public Mp3PcmStream(Bitstream bitstream) {
        this.bitstream = bitstream;
    }

    /**
     * Читает следующий байт данных из потока.
     * <p>
     * Основной метод декодировки. Его логика состоит из двух фаз:
     * <ol>
     *   <li><b>Проверка буфера</b>: если текущий буфер {@link #currentFrameBytes} пуст
     *       или исчерпан, загружается и декодируется следующий MP3-фрейм.</li>
     *   <li><b>Возврат байта</b>: возвращается байт по текущей позиции {@link #framePos},
     *       которая затем увеличивается.</li>
     * </ol>
     * </p>
     * <p>
     * <b>Декодировка фрейма</b>:
     * <ul>
     *   <li>Читается заголовок фрейма через {@link Bitstream#readFrame()}.</li>
     *   <li>Если заголовок <code>null</code>, устанавливается {@link #endOfStream} и возвращается <code>-1</code>.</li>
     *   <li>Фрейм декодируется в {@link SampleBuffer} с помощью {@link Decoder#decodeFrame(Header, Bitstream)}.</li>
     *   <li>16-битные сэмплы из {@code short[]} конвертируются в байты (little-endian) и копируются в {@link #currentFrameBytes}.</li>
     *   <li>Позиция сбрасывается на <code>0</code>.</li>
     * </ul>
     * </p>
     *
     * <h3>Пример потока данных</h3>
     * <pre>{@code
     * MP3 Frame (1152 samples) -> Decoder -> short[1152] -> byte[2304] -> по одному байту через read()
     * }</pre>
     *
     * @return следующий байт данных в диапазоне 0-255, или -1 при достижении конца потока
     * @throws IOException если возникает ошибка при чтении или декодировке MP3-фрейма
     * @see #read(byte[], int, int)
     * @see Bitstream#readFrame()
     * @see Decoder#decodeFrame(Header, Bitstream)
     * @since 0.1.3.0
     */
    @Override
    public int read() throws IOException {
        if (endOfStream)
            return -1;

        if (currentFrameBytes == null || framePos >= currentFrameBytes.length) {
            try {
                Header header = bitstream.readFrame();
                if (header == null) {
                    endOfStream = true;
                    return -1;
                }

                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);

                short[] buffer = output.getBuffer();
                int length = output.getBufferLength();
                currentFrameBytes = new byte[length * 2];

                for (int i = 0; i < length; i++) {
                    short val = buffer[i];
                    currentFrameBytes[i * 2] = (byte) (val & 0xFF);
                    currentFrameBytes[i * 2 + 1] = (byte) ((val >>> 8) & 0xFF);
                }

                framePos = 0;

                bitstream.closeFrame();
            } catch (BitstreamException | DecoderException e) {
                throw new IOException("Ошибка декодирования MP3", e);
            }
        }

        return currentFrameBytes[framePos++] & 0xFF;
    }

    /**
     * Читает до <code>len</code> байтов данных в указанный массив.
     * <p>
     * Это оптимизированная версия {@link #read()}, которая заполняет массив
     * за один вызов, минимизируя количество операций декодировки.
     * </p>
     * <p>
     * Метод делегирует чтение одиночных байтов методу {@link #read()}, что обеспечивает
     * согласованность логики, но может быть не самым эффективным способом.
     * </p>
     *
     * <h3>Поведение при EOF</h3>
     * <p>
     * Если конец потока достигнут <i>до</i> заполнения массива:
     * <ul>
     *   <li>Если ни одного байта не прочитано, возвращается <code>-1</code>.</li>
     *   <li>Иначе возвращается количество успешно прочитанных байтов.</li>
     * </ul>
     * </p>
     *
     * @param b массив, в который будут записаны байты
     * @param off смещение в массиве, с которого начинается запись
     * @param len максимальное количество байтов для чтения
     * @return количество прочитанных байтов, или -1 если ни одного байта не прочитано и достигнут конец потока
     * @throws IOException если возникает ошибка при чтении
     * @throws NullPointerException если массив <code>b</code> равен <code>null</code>
     * @throws IndexOutOfBoundsException если <code>off</code> или <code>len</code> некорректны
     * @see #read()
     * @since 0.1.3.0
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int i;

        for (i = 0; i < len; i++) {
            int val = read();

            if (val == -1) {
                return i == 0 ? -1 : i;
            }

            b[off + i] = (byte) val;
        }

        return i;
    }
}