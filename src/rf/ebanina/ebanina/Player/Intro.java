package rf.ebanina.ebanina.Player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.nio.file.Path;

/**
 * <h1>Intro</h1>
 * Класс для воспроизведения аудио‑интро перед основным треком медиаплеера.
 * <p>
 * Представляет интро как локальный файловый ресурс, реализует интерфейс {@link MediaReference},
 * позволяя получать путь к файлу и управлять его проигрыванием в отдельном потоке.
 * </p>
 * <p>
 * Поддерживает только 16‑битный PCM signed little‑endian аудио (стандартный формат WAV);
 * другой формат приводит к {@link UnsupportedOperationException}.
 * </p>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * // 1. Создание по файлу
 * Intro intro = new Intro(new File("intro.wav"));
 *
 * // 2. Создание по java.nio.Path
 * Intro intro2 = new Intro(Paths.get("intro.wav"));
 *
 * // 3. Создание по строке‑пути
 * Intro intro3 = new Intro("intro.wav");
 *
 * // 4. Проигрывание интро
 * intro.play();
 * }</pre>
 *
 * @author Ebanina Std
 * @since 0.1.4.4
 * @see MediaReference
 * @see Media#getIntroSoundFile()
 */
public class Intro
        implements MediaReference
{
    /**
     * Путь к файлу интро в формате строки.
     * <p>
     * Хранит абсолютный путь к audio‑файлу, используемому как интро. Может быть
     * установлен как через {@link File}, так и через {@link Path} или строку.
     * Поле доступно для наследующих классов (package + protected).
     * </p>
     *
     * @see #Intro(Path)
     * @see #Intro(File)
     * @see #Intro(String)
     * @see #setFileSource(File)
     * @see #setSource(String)
     * @since 0.1.4.4
     */
    protected String source;

    /**
     * Конструктор по умолчанию для создания пустого интро.
     * <p>
     * Не устанавливает путь к файлу; поле {@link #source} остаётся {@code null}
     * до вызова одного из методов установки источника.
     * </p>
     *
     * @since 0.1.4.4
     */
    public Intro() {}

    /**
     * Конструктор, принимающий путь к файлу интро в виде {@link Path}.
     * <p>
     * Преобразует {@link Path} в абсолютный путь к файлу и сохраняет его в поле
     * {@link #source}. Используется, когда источник задан через Java NIO.
     * </p>
     *
     * @param path путь к файлу интро; не должен быть {@code null}
     * @throws NullPointerException если {@code path} равен {@code null}
     * @since 0.1.4.4
     */
    public Intro(Path path) {
        this.source = path.toFile().getAbsolutePath();
    }

    /**
     * Конструктор, принимающий файл интро в виде {@link File}.
     * <p>
     * Сохраняет абсолютный путь к файлу в поле {@link #source}. Используется,
     * когда источник задан как экземпляр {@link File}.
     * </p>
     *
     * @param file объект файла интро; не должен быть {@code null}
     * @throws NullPointerException если {@code file} равен {@code null}
     * @since 0.1.4.4
     */
    public Intro(File file) {
        this.source = file.getAbsolutePath();
    }

    /**
     * Конструктор, принимающий путь к файлу интро в виде строки.
     * <p>
     * Сохраняет путь в поле {@link #source} без преобразования. Используется,
     * когда источник задан уже как абсолютный или относительный путь в строке.
     * </p>
     *
     * @param source путь к файлу интро в виде строки; не должен быть {@code null}
     * @throws NullPointerException если {@code source} равен {@code null}
     * @since 0.1.4.4
     */
    public Intro(String source) {
        this.source = source;
    }

    /**
     * Возвращает источник интро в виде объекта {@link File}.
     * <p>
     * Создаёт новый экземпляр {@link File} из текущего значения {@link #source}.
     * Если {@link #source} равен {@code null}, поведение соответствует {@link File#File(String)}.
     * </p>
     *
     * @return объект файла интро, соответствующий текущему пути
     * @see #source
     * @see #setFileSource(File)
     * @since 0.1.4.4
     */
    public File getFileSource() {
        return new File(source);
    }

    /**
     * Устанавливает источник интро из объекта {@link File}.
     * <p>
     * Сохраняет абсолютный путь файла в поле {@link #source} и возвращает
     * текущий экземпляр для поддержки fluent‑API.
     * </p>
     *
     * @param source объект файла интро; не должен быть {@code null}
     * @return текущий экземпляр {@link Intro} для chaining
     * @throws NullPointerException если {@code source} равен {@code null}
     * @see #source
     * @see #getFileSource()
     * @since 0.1.4.4
     */
    public Intro setFileSource(File source) {
        this.source = source.getAbsolutePath();
        return this;
    }

    /**
     * Возвращает строковый путь к файлу интро.
     * <p>
     * Возвращает текущее значение поля {@link #source} без изменений.
     * Может быть {@code null}, если конструктор без параметров использовался,
     * а источник не был установлен.
     * </p>
     *
     * @return строка‑путь к файлу интро или {@code null}
     * @see #source
     * @see #setSource(String)
     * @since 0.1.4.4
     */
    public String getSource() {
        return source;
    }

    /**
     * Устанавливает строковый путь к файлу интро.
     * <p>
     * Присваивает значение {@code source} полю {@link #source} и возвращает
     * текущий экземпляр для поддержки fluent‑API. Не выполняет проверок
     * на существование файла.
     * </p>
     *
     * @param source путь к файлу интро в виде строки; может быть относительным или абсолютным
     * @return текущий экземпляр {@link Intro} для chaining
     * @see #source
     * @see #getSource()
     * @since 0.1.4.4
     */
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

                        if (newSample > 32767)
                            newSample = 32767;
                        if (newSample < -32768)
                            newSample = -32768;

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

    /**
     * Запускает проигрывание интро в отдельном потоке.
     * <p>
     * Если внутренний {@link #introThread} не находится в состоянии {@link Thread.State#RUNNABLE}
     * (например, ещё не запущен или завершён), метод запускает поток через {@link Thread#start()}.
     * Если поток уже выполняется, метод не делает ничего.
     * </p>
     *
     * @see #introThread
     * @see #getIntroThread()
     * @since 0.1.4.4
     */
    public void play() {
        if(introThread.getState() != Thread.State.RUNNABLE) {
            introThread.start();
        }
    }

    /**
     * Возвращает объект потока, отвечающего за проигрывание интро.
     * <p>
     * Позволяет получать доступ к {@link Thread}, на котором работает интро, например,
     * для проверки состояния, отладки или интеграции в тесты. Изменения состояния потока
     * не должны выполняться напрямую вне класса.
     * </p>
     *
     * @return поток интро {@link #introThread}
     * @see #play()
     * @see #setIntroThread(Thread)
     * @since 0.1.4.4
     */
    public Thread getIntroThread() {
        return introThread;
    }

    /**
     * Устанавливает поток интро, заменяя текущий {@link #introThread}.
     * <p>
     * Метод нужен главным образом для тестирования или расширения поведения;
     * позволяет подменить поток интро другим экземпляром {@link Thread}.
     * Возвращает текущий экземпляр {@link Intro} для поддержки fluent‑API.
     * </p>
     *
     * @param introThread новый поток для проигрывания интро; не должен быть {@code null}
     * @return текущий экземпляр {@link Intro} для chaining
     * @throws NullPointerException если {@code introThread} равен {@code null}
     * @see #introThread
     * @see #getIntroThread()
     * @since 0.1.4.4
     */
    protected Intro setIntroThread(Thread introThread) {
        this.introThread = introThread;
        return this;
    }

    /**
     * Возвращает путь к файлу интро, реализуя контракт {@link MediaReference}.
     * <p>
     * Возвращает текущее значение поля {@link #source}, как путь к файлу интро.
     * Используется кодом, зависящим от интерфейса {@link MediaReference}
     * для получения места расположения медиа‑ресурса.
     * </p>
     *
     * @return путь к файлу интро или {@code null}, если он не установлен
     * @see MediaReference#getPath()
     * @see #source
     * @since 0.1.4.4
     */
    @Override
    public String getPath() {
        return source;
    }

    /**
     * Указывает, что интро загружается не из сетевого потока.
     * <p>
     * Всегда возвращает {@code false}, поскольку интро читается из локального файла
     * {@link File}, а не из сетевого источника (например, через Netty).
     * </p>
     *
     * @return {@code false} всегда, так как источник локальный
     * @see MediaReference#isNetty()
     * @since 0.1.4.4
     */
    @Override
    public boolean isNetty() {
        return false;
    }
}
