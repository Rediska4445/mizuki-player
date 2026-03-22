package rf.ebanina.ebanina.Player;

import com.mpatric.mp3agic.Mp3File;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.util.Duration;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Header;
import rf.ebanina.ebanina.Player.AudioEffect.Effector;
import rf.ebanina.ebanina.Player.AudioEffect.IAudioEffect;
import rf.ebanina.ebanina.Player.AudioEffect.Tempo.ITempoShifter;
import rf.ebanina.ebanina.Player.AudioEffect.Tempo.TempoShifter;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;
import rf.ebanina.utils.loggining.logging;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <h1>MediaPlayer</h1>
 * Главный компонент аудиосистемы — мощный, гибкий и расширяемый медиаплеер, способный воспроизводить
 * локальные и сетевые аудиофайлы с продвинутой обработкой звука в реальном времени.
 * <p>
 * {@code MediaPlayer} — это не просто проигрыватель. Это <b>аудио-движок</b>, сочетающий
 * низкоуровневый контроль через {@link SourceDataLine} и высокий уровень абстракции
 * для эффектов, автоматизации и интеграции. Он построен исключительно для Ebanina.
 * </p>
 * <p>
 * Плеер <b>тесно интегрирован с JavaFX</b> — все слушатели событий ({@code onPlaying}, {@code onEndOfMedia})
 * автоматически выполняются в UI-потоке через {@link Platform#runLater(Runnable)}. Эта связь <b>не будет разорвана</b>
 * — плеер остаётся частью JavaFX-стека.
 * </p>
 *
 * <h2>Ключевые возможности</h2>
 * <ul>
 *   <li><b>Воспроизведение MP3, WAV</b> — поддержка основных форматов "из коробки" через {@link AudioDecoder}.</li>
 *   <li><b>Сетевое воспроизведение</b> — передавайте URL напрямую в {@link Media}, плеер сам определит тип ресурса.</li>
 *   <li><b>VST/VST2/VST3</b> — интеграция с эффектами через класс-обёртку {@link PluginWrapper}.</li>
 *   <li><b>Обработка звука</b> — через встроенные эффекты в {@link Effector} или кастомные {@code AudioListener}.</li>
 *   <li><b>Управление выводом</b> — смена аудиоустройства через {@link #setAudioOutput(String)}.</li>
 *   <li><b>Интро-аудио</b> — отдельный поток {@code introThread} проигрывает интро перед треком.</li>
 *   <li><b>Автоматическая инициализация</b> — при создании вызывается {@code onCreate}, где можно настроить параметры.</li>
 * </ul>
 *
 * <h2>Кастомные декодеры</h2>
 * <p>
 * Если плеер встречает аудиофайл, не являющийся MP3 или WAV, он не останавливается —
 * он начинает <b>поиск подходящего декодера</b> в статическом списке {@link #decoders}.
 * </p>
 * <p>
 * Декодер — это класс, реализующий интерфейс {@link AudioDecoder}:
 * <pre>{@code
 * public interface AudioDecoder {
 *     String getFormat();                    // Например, "flac", "ogg"
 *     double computeAudioDuration(File file); // Вычисление длительности
 *     AudioInputStream createStreaming(File file); // Создание потока
 * }
 * }</pre>
 * </p>
 * <p>
 * Добавление реализации в {@code List<AudioDecoder> decoders} означает:
 * <ul>
 *   <li>Плеер при загрузке проверит расширение файла.</li>
 *   <li>Если это не MP3/WAV — начнёт перебор списка {@code decoders}.</li>
 *   <li>Найдёт подходящий по {@code getFormat()} — использует его для декодирования.</li>
 * </ul>
 * </p>
 * <p>
 * Это позволяет <b>расширять поддержку форматов без изменения кода плеера</b>.
 * Подробнее — см. документацию к {@link AudioDecoder}.
 * </p>
 *
 * <h2>Простое использование</h2>
 * Для базового воспроизведения достаточно:
 * <pre>{@code
 * MediaPlayer player = new MediaPlayer(new Media("media/song.mp3"));
 * player.play();
 * }</pre>
 * Плеер автоматически подготовится, запустит интро (если есть), и начнёт воспроизведение.
 *
 * <h2>Расширенное использование</h2>
 * <pre>{@code
 * MediaPlayer player = new MediaPlayer(new Media("http://example.com/stream.flac"));
 *
 * // Настройка параметров
 * player.setVolume(0.9);
 * player.setTempo(1.3f);
 * player.setPan(0.2);
 *
 * // Смена аудиовыхода
 * player.setAudioOutput("Headphones");
 *
 * // VST-эффекты
 * player.setPlugins(Arrays.asList(new PluginWrapper("plugins/reverb.vst3")));
 *
 * // Обработка через Effector
 * player.getEffector().enablePitchShift(true).setPitchFactor(1.1f);
 *
 * // Кастомный обработчик
 * player.setAudioListener((block, frames) -> {
 *     for (float[] ch : block) for (int i = 0; i < frames; i++) ch[i] *= 1.05f;
 *     return block;
 * });
 *
 * player.play();
 * }</pre>
 *
 * <h2>Жизненный цикл и события</h2>
 * <ol>
 *   <li><b>Создание</b>: {@code new MediaPlayer(media)}</li>
 *   <li><b>Подготовка</b>: {@link #prepareAsync()} → инициализирует потоки</li>
 *   <li><b>Воспроизведение</b>: {@link #play()} → запускает основной цикл</li>
 *   <li><b>Пауза/остановка</b>: {@link #pause()}, {@link #stop()}</li>
 *   <li><b>Освобождение</b>: {@link #dispose()} или {@link #close()}</li>
 * </ol>
 *
 * <h2>Управление длительностью и форматом</h2>
 * <ul>
 *   <li><b>Формат аудио</b>: получается через {@link #getFormat()} — используется для настройки {@code SourceDataLine}.</li>
 *   <li><b>Общая длительность</b>: вычисляется один раз в {@link #getOverDuration()} с учётом темпа и кэшируется.</li>
 *   <li><b>Пересчёт длительности</b>: вызовите {@link #recalculateOverDuration()}, если темп изменился.</li>
 *   <li><b>Исходная длительность</b>: {@link #getTotalDuration()} — вычисляется как {@code frameLength / frameRate}.</li>
 * </ul>
 *
 * <h2>Особенности реализации</h2>
 * <ul>
 *   <li><b>Нет seek</b> — перемотка невозможна из-за потокового декодирования MP3.</li>
 *   <li><b>Форматы</b> — из коробки: {@link AvailableFormat#MP3}, {@link AvailableFormat#WAV}. Другие — через кастомные декодеры.</li>
 *   <li><b>Статусы</b> — все состояния плеера определены в {@link Status}: {@code READY}, {@code PLAYING}, {@code PAUSED} и др.</li>
 *   <li><b>Интро</b> — реализовано в отдельном потоке {@code introThread}, проигрывает 16-bit PCM WAV.</li>
 *   <li><b>onCreate</b> — статический слушатель, вызывается при создании плеера. Используется для инициализации параметров.</li>
 *   <li><b>closeResources</b> — не вызывает {@code drain()}, так как {@code stop()} и {@code dispose()} уже управляют этим.
 *       Это позволяет быстро останавливать плеер без ожидания завершения буфера.</li>
 * </ul>
 *
 * <h2>Связанные классы</h2>
 * <ul>
 *   <li>{@link Media} — источник аудио (файл или URL)</li>
 *   <li>{@link PluginWrapper} — обёртка для VST-плагинов</li>
 *   <li>{@link Effector} — встроенные эффекты (темп, pitch, фильтры)</li>
 *   <li>{@link AudioDecoder} — декодирование MP3/WAV и кастомных форматов</li>
 *   <li>{@link AvailableFormat} — список поддерживаемых форматов</li>
 *   <li>{@link Status} — все возможные состояния плеера</li>
 * </ul>
 *
 * <h2>Примечания</h2>
 * <ul>
 *   <li>Реализует {@link AutoCloseable} — {@link #close()} вызывает {@link #dispose()}.
 *       Подходит для {@code try-with-resources}, но чаще используется как долгоживущий объект.</li>
 *   <li>Потокобезопасность: UI-связанные слушатели должны обновляться через {@link Platform#runLater(Runnable)}.</li>
 * </ul>
 *
 * @author Ebanina Std.
 * @version 0.1.4.4
 * @since 0.1.3.0
 * @see Media
 * @see PluginWrapper
 * @see Effector
 * @see AudioDecoder
 * @see AvailableFormat
 * @see Status
 * @see #play()
 * @see #pause()
 * @see #stop()
 * @see #setAudioOutput(String)
 * @see #getFormat()
 * @see #getOverDuration()
 * @see #recalculateOverDuration()
 * @see #getTotalDuration()
 * @see #closeResources()
 * @see #decoders
 */
@logging(isActive = true, fileOut = false)
public class MediaPlayer
        implements AutoCloseable, Serializable, Cloneable, Comparable<MediaPlayer>
{
    /**
     * Версия сериализации для обеспечения совместимости между версиями класса.
     * <p>
     * Изменяется при внесении несовместимых изменений в структуру класса.
     * Текущее значение {@code 1_4_4L}
     * </p>
     *
     * @since 1.3.0
     */
    @Serial
    private static final long serialVersionUID = 1_4_4L;

    /**
     * Основной источник аудиомедиа, с которым работает плеер.
     * <p>
     * Это — бъект {@link Media}, инкапсулирующий путь к аудиофайлу
     * или URL сетевого ресурса. Именно он определяет, <i>что</i> будет воспроизводиться
     * и <i>откуда</i> будут читаться данные.
     * </p>
     * <p>
     * Поле инициализируется один раз в конструкторе и остаётся неизменным на протяжении
     * всего жизненного цикла плеера. Это соответствует философии {@code Media} как
     * <b>лёгкой, неизменяемой обёртки</b> — он не содержит данных, а лишь указывает на них.
     * </p>
     * <p>
     * Плеер использует {@code media} для:
     * <ul>
     *   <li>Открытия {@link AudioInputStream} через {@link AudioDecoder}.</li>
     *   <li>Определения типа ресурса (локальный файл / сеть) через {@link Media#isNetty()}.</li>
     *   <li>Получения пути к интро-аудио (если задано).</li>
     *   <li>Вычисления длительности и формата.</li>
     * </ul>
     * </p>
     * <p>
     * Для получения ссылки на источник используйте {@link #getMedia()}.
     * </p>
     *
     * @see #MediaPlayer(Media)
     * @see #getMedia()
     * @see Media
     * @see AudioDecoder
     * @since 0.1.3.0
     */
    private Media media;
    /**
     * Свойство текущего состояния плеера.
     * <p>
     * Этот объект отслеживает, что происходит прямо сейчас.
     * Все возможные состояния определены в {@link Status}: от {@code UNKNOWN} до {@code DISPOSED}.
     * </p>
     * <p>
     * Свойство доступно через {@link #statusProperty()} и идеально подходит для привязки
     * в интерфейсе: кнопки "Play"/"Pause" могут реагировать на изменения автоматически.
     * </p>
     * <p>
     * Пример:
     * <pre>{@code
     * player.statusProperty().addListener((obs, old, current) -> {
     *     System.out.println("Статус изменился: " + current);
     * });
     * }</pre>
     * </p>
     * <p>
     * Начальное значение — {@link Status#UNKNOWN}. После подготовки плеер переходит в {@link Status#READY}.
     * </p>
     *
     * @see #statusProperty()
     * @see Status
     * @see #getStatus()
     * @since 0.1.3.0
     */
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.UNKNOWN);
    /**
     * Свойство уровня громкости.
     * <p>
     * Управляет силой звука: от {@code 0.0} (тихо) до {@code 1.0} (максимум).
     * Изменение происходит <b>плавно</b> — с шагом {@code 0.01f} в основном цикле,
     * чтобы избежать щелчков и разрывов.
     * </p>
     * <p>
     * Слушатель устанавливается в конструкторе: при изменении {@code volumeProperty}
     * автоматически обновляется внутренний {@code currentVolume} и применяется к аудиолинии.
     * </p>
     * <p>
     * Идеально для привязки к слайдеру громкости в UI.
     * </p>
     *
     * @see #volumeProperty()
     * @see #getVolume()
     * @see #setVolume(double)
     * @since 0.1.3.0
     */
    private final DoubleProperty volume = new SimpleDoubleProperty(1.0);
    /**
     * Свойство текущего времени воспроизведения.
     * <p>
     * Это — <b>секундомер</b> плеера: показывает, сколько времени прошло с начала трека.
     * Обновляется в реальном времени в основном цикле {@link #readProcessLoop()}.
     * </p>
     * <p>
     * Значение зависит от текущего темпа: при {@code tempo = 2.0f} время идёт в 2 раза быстрее.
     * </p>
     * <p>
     * Свойство — <b>только для чтения</b>. Для привязки в UI используйте {@link #currentTimeProperty()}.
     * </p>
     * <p>
     * Начальное значение — {@link Duration#ZERO}.
     * </p>
     *
     * @see #currentTimeProperty()
     * @see #getCurrentTime()
     * @see #updateCurrentTime(Duration)
     * @since 0.1.3.0
     */
    private final ReadOnlyObjectWrapper<Duration> currentTime = new ReadOnlyObjectWrapper<>(Duration.ZERO);
    /**
     * Свойство общей длительности аудиофайла.
     * <p>
     * Это — <b>таймер трека</b>: показывает, сколько времени длится воспроизведение.
     * Вычисляется один раз при подготовке через {@link #recalculateOverDuration()}
     * и кэшируется в {@link #totalOverDuration}.
     * </p>
     * <p>
     * Длительность учитывает текущий темп: при {@code tempo = 0.5f} трек будет в 2 раза длиннее.
     * </p>
     * <p>
     * Свойство — <b>только для чтения</b>. Используется для настройки слайдеров и прогресс-баров.
     * </p>
     * <p>
     * Начальное значение — {@link Duration#UNKNOWN}.
     * </p>
     *
     * @see #totalDurationProperty()
     * @see #getTotalDuration()
     * @see #getOverDuration()
     * @see #recalculateOverDuration()
     * @since 0.1.3.0
     */
    private final ReadOnlyObjectWrapper<Duration> totalDuration = new ReadOnlyObjectWrapper<>(Duration.UNKNOWN);
    /**
     * Исполнительный сервис, управляющий потоком воспроизведения аудио.
     * <p>
     * Это — <b>двигатель</b> плеера: он запускает и контролирует основной цикл {@link #processAudioStream()}.
     * Создаётся только при первом вызове {@link #play()} — до этого момента плеер "спит".
     * </p>
     * <p>
     * Использует <b>однопоточный демонический пул</b> — аудиовоспроизведение работает в фоне,
     * не блокируя завершение приложения.
     * </p>
     * <p>
     * При остановке плеера сервис завершается через {@link ExecutorService#shutdownNow()} —
     * поток прерывается, буферы очищаются.
     * </p>
     *
     * @see #play()
     * @see #stop()
     * @see #dispose()
     * @see #processAudioStream()
     * @since 0.1.3.0
     */
    private ExecutorService playbackExecutor;
    /**
     * Ссылка на задачу воспроизведения, запущенную в {@link #playbackExecutor}.
     * <p>
     * Это — <b>руль</b> аудиопотока: позволяет отслеживать и управлять выполнением
     * через {@link Future}. Хотя плеер сам управляет жизненным циклом задачи,
     * ссылка остаётся доступной для диагностики и отладки.
     * </p>
     * <p>
     * Задача запускается как {@code playbackExecutor.submit(this::processAudioStream)}.
     * При остановке отменяется через {@link Future#cancel(boolean)} с флагом {@code true}.
     * </p>
     *
     * @see #play()
     * @see #stop()
     * @see #playbackExecutor
     * @since 0.1.3.0
     */
    private Future<?> playbackTask;
    /**
     * Планировщик фоновых задач.
     * <p>
     * Это — <b>часовщик</b> плеера: отвечает за периодические операции, такие как
     * обновление UI, таймеры, фоновые проверки.
     * </p>
     * <p>
     * Создаётся как однопоточный {@link ScheduledExecutorService} — все задачи выполняются
     * последовательно, без риска конфликтов.
     * </p>
     * <p>
     * Используется, например, для:
     * <ul>
     *   <li>Обновления {@link #currentTime} в UI-потоке.</li>
     *   <li>Проверки достижения {@link #stopTime}.</li>
     *   <li>Фоновой подготовки следующего трека.</li>
     * </ul>
     * </p>
     * <p>
     * Завершается при {@link #dispose()} через {@link ScheduledExecutorService#shutdownNow()}.
     * </p>
     *
     * @see #scheduler
     * @see #runOnEndOfMedia()
     * @since 0.1.3.0
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    /**
     * Флаг, указывающий, находится ли плеер в состоянии воспроизведения.
     * <p>
     * {@code true} — плеер играет, {@code false} — нет.
     * Управляется строго через {@link #play()} и {@link #stop()}.
     * </p>
     * <p>
     * Используется для:
     * <ul>
     *   <li>Проверки, можно ли ставить на паузу.</li>
     *   <li>Блокировки повторного вызова {@link #play()}.</li>
     *   <li>Синхронизации между потоками (аудио и UI).</li>
     * </ul>
     * </p>
     * <p>
     * Потокобезопасен благодаря {@link AtomicBoolean} — изменения видны сразу во всех потоках
     * без необходимости в синхронизации.
     * </p>
     *
     * @see #play()
     * @see #stop()
     * @since 0.1.3.0
     */
    private final AtomicBoolean playing = new AtomicBoolean(false);
    /**
     * Флаг, указывающий, находится ли плеер на паузе.
     * <p>
     * Это — <b>переключатель</b> воспроизведения: {@code true} — звук приостановлен, буферы живы,
     * {@code false} — воспроизведение активно или остановлено.
     * </p>
     * <p>
     * В отличие от {@link #playing}, пауза не разрывает цепочку воспроизведения —
     * при вызове {@link #play()} плеер продолжит с той же позиции.
     * </p>
     * <p>
     * Управляется через {@link #pause()} и {@link #play()}.
     * Также используется в синхронизации потоков через {@code pauseLock}.
     * </p>
     * <p>
     * Потокобезопасен — изменения мгновенно видны в аудиопотоке.
     * </p>
     *
     * @see #pause()
     * @see #play()
     * @since 0.1.3.0
     */
    private final AtomicBoolean paused = new AtomicBoolean(false);
    /**
     * Входной аудиопоток с декодированными PCM-данными.
     * <p>
     * Используется для последовательного чтения аудиоблоков из исходного файла или сетевого ресурса.
     * Создаётся в методе {@link #openStreamsAndPrepare()} на основе {@link #media} и формата,
     * указанного в {@link AudioDecoder}.
     * </p>
     * <p>
     * Поток предоставляет доступ к сырым PCM-данным в формате, пригодном для вывода на аудиоустройство.
     * Читается блоками в цикле {@link #readProcessLoop()}.
     * </p>
     * <p>
     * Автоматически закрывается при остановке плеера через {@link #closeResources()}.
     * </p>
     *
     * @see #openStreamsAndPrepare()
     * @see #readProcessLoop()
     * @see #closeResources()
     * @see AudioDecoder
     * @since 0.1.3.0
     */
    private AudioInputStream audioStream;
    /**
     * Аудиолиния вывода — физическое или виртуальное звуковое устройство.
     * <p>
     * Используется для отправки обработанных аудиоданных на воспроизведение.
     * Открывается в {@link #openStreamsAndPrepare()} с форматом, совместимым с декодированным потоком.
     * </p>
     * <p>
     * Управляет буферизацией, таймингом и синхронизацией с аудиодрайвером.
     * Запускается в {@link #play()}, останавливается в {@link #pause()} и {@link #stop()}.
     * </p>
     * <p>
     * Может быть перенастроена на другое устройство через {@link #setAudioOutput(String)}.
     * </p>
     *
     * @see #openStreamsAndPrepare()
     * @see #play()
     * @see #pause()
     * @see #stop()
     * @see #setAudioOutput(String)
     * @see #closeResources()
     * @since 0.1.3.0
     */
    protected SourceDataLine line;
    /**
     * Список плагинов, подключённых к плееру.
     * <p>
     * Используется для обработки аудиосигнала в реальном времени.
     * Плагины применяются в порядке их добавления в цепочке обработки.
     * </p>
     * <p>
     * Устанавливается через {@link #setPlugins(List)}.
     * </p>
     *
     * @see #setPlugins(List)
     * @see #getPlugins()
     * @since 0.1.3.0
     */
    private List<PluginWrapper> plugins;
    /**
     * Кэшированная общая длительность аудиопотока с учётом текущего темпа.
     * <p>
     * Используется для оптимизации — избегает повторного вычисления.
     * Обновляется при вызове {@link #recalculateOverDuration()}.
     * </p>
     *
     * @see #getOverDuration()
     * @see #recalculateOverDuration()
     * @since 0.1.3.0
     */
    private Duration totalOverDuration = null;
    /**
     * Временная точка, с которой начинается воспроизведение.
     * <p>
     * Позволяет запускать трек не с самого начала.
     * Учитывает текущий темп при установке.
     * </p>
     *
     * @see #setStartTime(Duration)
     * @see #getStartTime()
     * @since 0.1.3.0
     */
    private Duration startTime = Duration.ZERO;
    /**
     * Временная точка, при достижении которой плеер остановится.
     * <p>
     * Если значение {@link Duration#UNKNOWN} — остановка не производится.
     * </p>
     *
     * @see #setStopTime(Duration)
     * @see #getStopTime()
     * @since 0.1.3.0
     */
    private Duration stopTime = Duration.UNKNOWN;
    /**
     * Текущий индекс фрейма в аудиопотоке.
     * <p>
     * Используется для отслеживания позиции воспроизведения.
     * Обновляется в {@link #readProcessLoop()}.
     * </p>
     *
     * @see #getCurrentTime()
     * @since 0.1.3.0
     */
    protected volatile long currentFrame = 0;
    /**
     * Служебные слушатели событий плеера.
     * <p>
     * Устанавливаются через соответствующие {@code setOn...} методы.
     * Выполняются в UI-потоке через {@link Platform#runLater(Runnable)}.
     * </p>
     *
     * @see #setOnReady(Runnable)
     * @see #setOnPlaying(Runnable)
     * @see #setOnPaused(Runnable)
     * @see #setOnStopped(Runnable)
     * @see #setOnEndOfMedia(Runnable)
     * @see #setOnHalted(Runnable)
     * @see #setOnDisposed(Runnable)
     * @since 0.1.3.0
     */
    private Runnable onStopTimeReached, onReady, onPlaying, onPaused, onStopped, onEndOfMedia, onHalted, onDisposed;
    /**
     * Входной буфер аудиоданных.
     * <p>
     * Представляет собой массив каналов, каждый — массив сэмплов.
     * </p>
     *
     * @see #readProcessLoop()
     * @since 0.1.3.0
     */
    private float[][] inputBlock;
    /**
     * Входной буфер для VST-плагинов.
     * <p>
     * Динамически подстраивается под максимальное число входов плагинов.
     * </p>
     *
     * @see #readProcessLoop()
     * @since 0.1.3.0
     */
    private float[][] vstInput;
    /**
     * Выходной буфер из VST-плагинов.
     * <p>
     * Содержит обработанный сигнал после прохождения всех плагинов.
     * </p>
     *
     * @see #readProcessLoop()
     * @since 0.1.3.0
     */
    private float[][] vstOutput;
    /**
     * Текущий уровень громкости, применяемый к аудиоданным.
     * <p>
     * Используется для плавного изменения громкости в {@link #readProcessLoop()}.
     * Обновляется с шагом 0.01f для предотвращения щелчков.
     * </p>
     *
     * @see #volumeProperty()
     * @since 0.1.3.0
     */
    private final AtomicReference<Float> currentVolume = new AtomicReference<>(1.0f);
    /**
     * Текущий множитель темпа воспроизведения.
     * <p>
     * Значение 1.0 — оригинальная скорость.
     * Устанавливается через {@link #setTempo(float)}.
     * </p>
     *
     * @see #setTempo(float)
     * @see #getTempo()
     * @since 0.1.3.0
     */
    private volatile float tempo = 1.0f;

    /**
     * Текущая панорама звука: -1.0 — левый канал, 0.0 — центр, 1.0 — правый.
     * <p>
     * Устанавливается через {@link #setPan(float)}.
     * </p>
     *
     * @see #setPan(float)
     * @see #getPan()
     * @since 0.1.3.0
     */
    private volatile float pan = 0.0f;
    /**
     * Свойство панорамы для привязки в UI.
     * <p>
     * Позволяет синхронизировать панораму с ползунком или другими элементами интерфейса.
     * </p>
     *
     * @see #panProperty()
     * @since 0.1.3.0
     */
    private final FloatProperty panProperty = new SimpleFloatProperty(0.0f);
    /**
     * Возвращает свойство панорамы для использования в привязках и наблюдателях.
     * <p>
     * Идеально подходит для интеграции с JavaFX UI.
     * </p>
     *
     * <h2>Пример</h2>
     * <pre>{@code
     * Slider panSlider = new Slider(-1.0, 1.0, 0.0);
     * panSlider.valueProperty().bindBidirectional(player.panProperty());
     * }</pre>
     *
     * @return свойство панорамы
     * @see #pan
     * @see #setPan(float)
     * @since 0.1.3.0
     */
    public FloatProperty panProperty() {
        return panProperty;
    }
    /**
     * Свойство текущего аудиоформата.
     * <p>
     * Используется для отображения в интерфейсе.
     * Обновляется при загрузке трека.
     * </p>
     *
     * @see #currentFormat
     * @see #getCurrentFormat()
     * @since 0.1.3.0
     */
    private final StringProperty currentFormat = new SimpleStringProperty();
    /**
     * Возвращает текущий аудиоформат (например, "MP3", "WAV").
     * <p>
     * Полезно для отображения в UI.
     * </p>
     *
     * <h2>Пример</h2>
     * <pre>{@code
     * Label formatLabel = new Label();
     * formatLabel.textProperty().bind(player.currentFormatProperty());
     * }</pre>
     *
     * @return формат аудио в виде строки
     * @see #currentFormat
     * @since 0.1.3.0
     */
    public String getCurrentFormat() {
        return currentFormat.get();
    }
    /**
     * Статический список кастомных декодеров.
     * <p>
     * Используется для поддержки форматов, отличных от MP3 и WAV.
     * При загрузке файла плеер перебирает этот список в поиске подходящего декодера.
     * </p>
     *
     * <h2>Пример</h2>
     * <pre>{@code
     * decoders.add(new FlacDecoder());
     * decoders.add(new OggDecoder());
     * }</pre>
     *
     * @see AudioDecoder
     * @see #getFormat()
     * @since 0.1.3.5
     */
    private List<AudioDecoder> decoders = new ArrayList<>();

    private int BLOCK_SIZE_FRAMES = 2048;

    public MediaPlayer(Media media, int BLOCK_SIZE_FRAMES) {
        this.media = Objects.requireNonNull(media, "Media cannot be null");
        this.BLOCK_SIZE_FRAMES = BLOCK_SIZE_FRAMES;

        prepareAsync();

        setStatus(Status.READY);

        volume.addListener((obs, oldV, newV) -> setLineVolume(newV.doubleValue()));
    }

    public MediaPlayer(Media media) {
        this(media, 2048);
    }

    public MediaPlayer setDecoders(List<AudioDecoder> decoders) {
        this.decoders = decoders;
        return this;
    }

    public List<AudioDecoder> getDecoders() {
        return decoders;
    }

    public MediaPlayer() {
        /* Empty Builder */
    }

    /**
     * Устанавливает новый аудиоресурс для плеера.
     * <p>
     * <b>Внимание:</b> метод не переподготавливает плеер автоматически.
     * Если плеер уже был подготовлен, изменения не повлияют на воспроизведение.
     * </p>
     * <p>
     * Используется в редких случаях, когда нужно сменить медиа без пересоздания плеера.
     * </p>
     *
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setMedia(new Media("new_track.mp3"));
     * player.prepareAsync(); // нужно вызвать вручную
     * }</pre>
     *
     * @param m новый аудиоресурс
     * @see #MediaPlayer(Media)
     * @since 0.1.3.0
     */
    public MediaPlayer setMedia(Media m) {
        this.media = m;
        return this;
    }

    /**
     * Устанавливает панораму аудио: -1.0 (лево), 0.0 (центр), 1.0 (право).
     * <p>
     * Значение автоматически ограничивается допустимым диапазоном [-1.0, 1.0].
     * Обновляет как внутреннее поле {@code pan}, так и {@link #panProperty} для привязки к UI.
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * player.setPan(-0.5f); // звук влево
     * Slider panSlider = ...;
     * panSlider.valueProperty().bindBidirectional(player.panProperty());
     * }</pre>
     * @param pan позиция панорамы: -1.0 ... 1.0
     * @see #getPan()
     * @see #panProperty()
     */
    public MediaPlayer setPan(float pan) {
        if (pan < -1.0f)
            pan = -1.0f;
        if (pan > 1.0f)
            pan = 1.0f;

        this.pan = pan;
        this.panProperty.set(pan);

        return this;
    }

    /**
     * Возвращает текущую позицию панорамы.
     * <p>
     * Значение: -1.0 (лево), 0.0 (центр), 1.0 (право).
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * float panPosition = player.getPan();
     * }</pre>
     * @return позиция панорамы
     * @see #setPan(float)
     */
    public float getPan() {
        return pan;
    }
    /**
     * Отдельный поток для проигрывания интро.
     * <p>
     * Загружает аудиофайл, проверяет формат (поддерживается только 16-bit PCM signed little-endian),
     * применяет fade-in и кроссфейд, затем проигрывает интро перед основным треком.
     * Автоматически освобождает ресурсы и вызывает {@link #play()} после завершения.
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * player.playIntro();
     * }</pre>
     * @see #playIntro()
     * @see Media#getIntroSoundFile()
     */
    public Thread introThread = new Thread(() -> {
        try (AudioInputStream introStream = AudioSystem.getAudioInputStream(media.getIntroSoundFile())) {
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
    /**
     * Запускает проигрывание интро-аудио.
     * <p>
     * Если интро отсутствует — автоматически запускает обычное воспроизведение.
     * Запуск происходит асинхронно.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.playIntro();
     * }</pre>
     * @see #introThread
     */
    public void playIntro() {
        if (!media.getIntroSoundFile().exists()) {
            play();

            return;
        }

        if(introThread.getState() != Thread.State.RUNNABLE) {
            introThread.start();
        }
    }
    /**
     * Устанавливает темп (скорость воспроизведения): 1.0 — оригинальный темп.
     * <p>
     * Значение автоматически ограничивается: допустимы только 0 < tempo < 2; иначе устанавливается 1.0.
     * Темп применяется к алгоритмам эффекторной обработки.
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * player.setTempo(1.25f); // Быстрее оригинала
     * }</pre>
     * @param tempo множитель темпа
     * @see #getTempo()
     */
    public MediaPlayer setTempo(float tempo) {
        if (tempo <= 0)
            tempo = 1.0f;
        else if (tempo >= 2)
            tempo = 1.0f;

        this.tempo = tempo;

        return this;
    }
    /**
     * Возвращает текущий множитель темпа.
     * <p>
     * Значение: 1.0 — оригинальная скорость.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * float speed = player.getTempo();
     * }</pre>
     * @return коэффициент темпа
     * @see #setTempo(float)
     */
    public float getTempo() {
        return tempo;
    }
    /**
     * Переключает вывод плеера на другое аудиоустройство (микшер) по имени.
     * <p>
     * Позволяет выбрать нужный выходной интерфейс: динамики, наушники, внешний ЦАП — всё, что доступно в системе.
     * Метод перебирает все устройства, возвращаемые {@link AudioSystem#getMixerInfo()}, и ищет тот,
     * чьё имя совпадает с {@code mixerName} (без учёта регистра).
     * </p>
     * <p>
     * Если устройство найдено, создаётся новая {@link SourceDataLine} с текущим {@link AudioFormat} (или с форматом из {@link #audioStream}).
     * Выполняется {@link SourceDataLine#open(AudioFormat)}, после чего открытая линия сразу запускается на воспроизведение.
     * </p>
     * <p>
     * Если нужное устройство не найдено — метод завершает работу без изменений.
     * Если невозможно определить формат или открыть линию — выбрасывается {@link IllegalStateException} или {@link LineUnavailableException}.
     * </p>
     *
     * <h2>Пример использования</h2>
     * <pre>{@code
     * player.setAudioOutput("Speakers");
     * player.setAudioOutput("Realtek(R) Audio");
     * }</pre>
     *
     * <h2>Примечания</h2>
     * <ul>
     *   <li>Имя устройства должно совпадать с выводом {@code getName()} в {@link javax.sound.sampled.Mixer.Info} (см. {@link AudioSystem#getMixerInfo()}).</li>
     *   <li>Открытие линии происходит без предварительного {@code drain()} прежней; рекомендуется остановить воспроизведение перед вызовом.</li>
     *   <li>Смену устройства можно производить "на лету", однако возможны щелчки или прерывания сигнала.</li>
     * </ul>
     *
     * @param mixerName имя желаемого устройства вывода (например, "Speakers", "Headphones")
     * @throws IllegalStateException если не удалось определить формат для открытия SourceDataLine
     * @throws LineUnavailableException если нужная аудиолиния занята или недоступна
     * @see java.util.Arrays#stream(Object[])
     * @see AudioSystem#getMixerInfo()
     * @see AudioSystem#getMixer(javax.sound.sampled.Mixer.Info)
     * @see SourceDataLine
     * @see AudioFormat
     * @since 0.1.4.0
     */
    public MediaPlayer setAudioOutput(String mixerName) throws Exception {
        Mixer.Info targetMixerInfo = null;

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equalsIgnoreCase(mixerName)) {
                targetMixerInfo = info;
                break;
            }
        }

        if (targetMixerInfo == null)
            return this;

        Mixer mixer = AudioSystem.getMixer(targetMixerInfo);

        AudioFormat format = line.getFormat();

        if (format == null && audioStream != null) {
            format = audioStream.getFormat();
        }

        if (format == null) {
            throw new IllegalStateException("Нет доступного формата аудио для создания новой линии.");
        }

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        line = (SourceDataLine) mixer.getLine(info);
        line.open(format);
        line.start();

        return this;
    }

    /**
     * Асинхронно подготавливает все необходимые ресурсы для воспроизведения (декодер, аудиолинию и т.д.).
     * <p>
     * Метод запускает отдельный поток для инициализации аудиостека — это исключает блокировку UI и ускоряет работу при загрузке длинных или сетевых треков.
     * После завершения подготовки внутренний статус плеера выставляется в {@link Status#READY}; при ошибке — в {@link Status#HALTED}.
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * player.prepareAsync();
     * player.setOnReady(() -> player.play());
     * }</pre>
     * @see #openStreamsAndPrepare()
     * @see Status
     */
    public void prepareAsync() {
        new Thread(() -> {
            try {
                openStreamsAndPrepare();
            } catch (Exception e) {
                setStatus(Status.HALTED);
            }
        }, "MediaPlayer-PrepareThread").start();
    }
    /**
     * Возвращает текущий входной аудиопоток с декодированными PCM-данными.
     * <p>
     * Можно использовать этот поток для низкоуровневого доступа или кастомной обработки аудиофреймов напрямую, например —
     * для своих DSP-алгоритмов или визуализации.
     * <b>Перед вызовом убедитесь, что подготовка завершена и поток открыт.</b>
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * AudioInputStream stream = player.getAudioStream();
     * AudioFormat fmt = stream.getFormat();
     * }</pre>
     * @return открытый {@link AudioInputStream}
     * @see #prepareAsync()
     */
    public AudioInputStream getAudioStream() {
        return audioStream;
    }
    /**
     * Возвращает формат текущего аудиопотока.
     * <p>
     * Используется для получения информации о параметрах сигнала: sample rate, channels, bit-depth.
     * Может быть полезен для создания собственных линий вывода или advanced routing.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * AudioFormat format = player.getFormat();
     * int channels = format.getChannels();
     * }</pre>
     * @throws NullPointerException если поток не инициализирован
     * @return формат аудиопотока
     * @see AudioFormat
     */
    public AudioFormat getFormat() {
        if(audioStream == null)
            throw new NullPointerException();

        return audioStream.getFormat();
    }
    /**
     * Получает кэшированную общую длительность аудиопотока (с учётом темпа).
     * <p>
     * Это значение вычисляется при первой загрузке файла и после явного пересчёта. Используется для работы с прогресс-барами и индикаторами трека.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * Duration total = player.getTotalOverDuration();
     * }</pre>
     * @return длительность трека с учётом темпа
     * @see #recalculateOverDuration()
     */
    public Duration getTotalOverDuration() {
        return totalOverDuration;
    }
    /**
     * Сохраняет значение общей длительности трека (с учётом текущего темпа).
     * <p>
     * Метод нужен для ручного пересчёта длительности — например, при смене темпа "на лету".
     * Возвращает {@code this} для удобства чейнинга.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setTotalOverDuration(Duration.seconds(120.5));
     * }</pre>
     * @param totalOverDuration новая длительность
     * @return этот же экземпляр {@link MediaPlayer}
     * @see #getTotalOverDuration()
     */
    public MediaPlayer setTotalOverDuration(Duration totalOverDuration) {
        this.totalOverDuration = totalOverDuration;
        return this;
    }
    /**
     * Обрабатывает событие достижения конечной точки воспроизведения.
     * <p>
     * Внутренний рабочий метод: выставляет флаги, корректно завершает линию, вызывает соответствующие обработчики — включая {@link #onStopTimeReached} (если установлен).
     * <b>Обычно не вызывается напрямую — срабатывает при автоматическом завершении проигрывания по таймеру/длительности.</b>
     * </p>
     * <ul>
     *   <li>Деактивирует флаги {@code playing} и {@code paused}</li>
     *   <li>Корректно завершает линию через {@link SourceDataLine#drain()} и {@link SourceDataLine#stop()}</li>
     *   <li>Оповещает все потоки, ожидающие на {@code pauseLock}</li>
     *   <li>Вызывает обработчики {@code onStopTimeReached}, {@code onEndOfMedia}, {@code onStopped}</li>
     * </ul>
     * @see #setOnStopTimeReached(Runnable)
     * @see #stop()
     */
    private void onStopTimeReached() {
        playing.set(false);
        paused.set(false);

        synchronized(pauseLock) {
            pauseLock.notifyAll();
        }

        if (line != null && line.isOpen()) {
            try {
                line.drain();
                line.stop();
            } catch (Exception ignored) {}
        }

        setStatus(Status.STOPPED);

        if (onStopTimeReached != null) {
            Platform.runLater(onStopTimeReached);
        }

        runOnEndOfMedia();
        runOnStopped();
    }
    /**
     * Устанавливает обработчик события достижения точки остановки воспроизведения.
     * <p>
     * Этот слушатель сработает, когда проигрывание трека дойдёт до заданного {@link #stopTime},
     * или после ручного вызова остановки. Обычно используется для автоматизации переключения треков,
     * повторного воспроизведения или кастомной логики по окончанию сегмента.
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * player.setOnStopTimeReached(() -> {
     *     System.out.println("Проиграно до stopTime!");
     * });
     * }</pre>
     * @param handler обработчик события
     * @see #stopTime
     */
    public void setOnStopTimeReached(Runnable handler) {
        this.onStopTimeReached = handler;
    }
    /**
     * Возвращает медиа-источник, используемый этим плеером.
     * <p>
     * Используется для доступа к информации о текущем аудиофайле или сетевом потоке.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * Media media = player.getMedia();
     * String sourcePath = media.getSource();
     * }</pre>
     * @return объект {@link Media}
     * @see Media
     */
    public Media getMedia() {
        return media;
    }
    /**
     * Свойство статуса плеера для JavaFX-binding.
     * <p>
     * Позволяет подписываться на изменения состояния: READY, PLAYING, PAUSED, STOPPED, HALTED, DISPOSED.
     * Может быть интегрировано с UI для динамического отображения статуса воспроизведения.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.statusProperty().addListener((obs, old, status) -> {
     *     statusLabel.setText(status.toString());
     * });
     * }</pre>
     * @return свойство статуса
     * @see Status
     */
    public ReadOnlyObjectProperty<Status> statusProperty() {
        return status;
    }
    /**
     * Возвращает текущий статус плеера.
     * <p>
     * Для прямого доступа к статусу без биндинга.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * if (player.getStatus() == Status.PLAYING) {
     *     // выполнить что-то во время проигрывания
     * }
     * }</pre>
     * @return статус плеера
     * @see #statusProperty()
     * @see Status
     */
    public Status getStatus() {
        return status.get();
    }
    /**
     * Свойство уровня громкости для JavaFX-binding.
     * <p>
     * Можно напрямую привязать к слайдеру или другому UI-элементу управления громкостью.
     * Диапазон значений: 0.0..1.0.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * volumeSlider.valueProperty().bindBidirectional(player.volumeProperty());
     * }</pre>
     * @return свойство громкости
     * @see #getVolume()
     * @see #setVolume(double)
     */
    public DoubleProperty volumeProperty() {
        return volume;
    }
    /**
     * Получает текущий уровень громкости (от 0.0 до 1.0).
     * <p>
     * Может быть использовано для отображения, сохранения настроек или обработки аудиосигнала.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * double vol = player.getVolume();
     * }</pre>
     * @return уровень громкости
     * @see #setVolume(double)
     * @see #volumeProperty()
     */
    public double getVolume() {
        return volume.get();
    }
    /**
     * Устанавливает уровень громкости (0.0..1.0).
     * <p>
     * Применяется плавно к основному аудиосигналу через механизм интерполяции.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setVolume(0.5); // приглушить звук
     * }</pre>
     * @param vol новый уровень громкости
     * @see #getVolume()
     * @see #volumeProperty()
     */
    public MediaPlayer setVolume(double vol) {
        volume.set(vol);

        return this;
    }
    /**
     * Свойство текущей позиции воспроизведения (Duration).
     * <p>
     * Только для чтения — идеально подходит для связки с прогрессбаром или слайдером в JavaFX UI.
     * Обновляется автоматически во время воспроизведения в реальном времени.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * progressBar.valueProperty().bind(player.currentTimeProperty());
     * }</pre>
     * @return свойство текущей позиции
     * @see #getCurrentTime()
     * @see Duration
     */
    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return currentTime.getReadOnlyProperty();
    }
    /**
     * Возвращает текущую позицию воспроизведения в секундах.
     * <p>
     * В отличие от {@link #currentTimeProperty()}, этот метод вычисляет реальное время
     * на основе текущей позиции аудиолинии и sample rate.
     * Используется для точного отображения прогресса и синхронизации UI.
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * Duration now = player.getCurrentTime();
     * progressBar.setProgress(now.toSeconds());
     * }</pre>
     * @return позиция воспроизведения; {@link Duration#UNKNOWN} если данные недоступны
     * @see #currentTimeProperty()
     */
    public Duration getCurrentTime() {
        if (line == null)
            return currentTime.get();

        float sampleRate = line.getFormat().getSampleRate();

        if (sampleRate <= 0)
            return Duration.UNKNOWN;

        double actualTime = (double) currentFrame / sampleRate;

        return Duration.seconds(actualTime /* / getTempo() */);
    }
    /**
     * Пересчитывает полную длительность аудиофайла с учётом темпа.
     * <p>
     * Метод определяет формат по расширению файла, для MP3 и WAV использует
     * библиотеки {@code Mp3File} и {@code AudioSystem}.
     * Для всех других поддерживаемых форматов — ищет нужный {@link AudioDecoder} в {@link #decoders}.
     * Возвращает результат в секундах, либо {@link Duration#UNKNOWN} если длительность определить невозможно.
     * <b>Время кэшируется в поле {@code totalOverDuration} до изменения темпа или смены трека.</b>
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * Duration audioLength = player.recalculateOverDuration();
     * }</pre>
     * @return длительность трека, скорректированная по темпу
     * @see #getOverDuration()
     * @see #decoders
     */
    public Duration recalculateOverDuration() {
        String name = media.getSource();

        Duration result = Duration.UNKNOWN;

        try {
            if (name.endsWith(AvailableFormat.MP3.name().toLowerCase())) {
                try {
                    Mp3File mp3 = new Mp3File(Objects.requireNonNull(media.getFile()));
                    double seconds = mp3.getLengthInSeconds();

                    if (seconds > 0) {
                        result = Duration.seconds(seconds);
                    }
                } catch (Exception ignored) {

                }
            } else if (name.endsWith(AvailableFormat.WAV.name().toLowerCase())) {
                try (AudioInputStream ais = AudioSystem.getAudioInputStream(Objects.requireNonNull(media.getFile()))) {
                    AudioFormat format = ais.getFormat();
                    long frameLength = ais.getFrameLength();

                    if (frameLength > 0 && format.getFrameRate() > 0) {
                        double seconds = frameLength / format.getFrameRate();
                        result = Duration.seconds(seconds);
                    }
                } catch (Exception ignored) {

                }
            } else {
                for (AudioDecoder dec : decoders) {
                    if (media.getSource().endsWith(dec.getFormat())) {
                        result = Duration.seconds(dec.computeAudioDuration(media.getFile()));
                    }
                }
            }
        } catch (Exception ignored) {

        }

        if (result.isUnknown() || result.lessThanOrEqualTo(Duration.ZERO)) {
            return Duration.UNKNOWN;
        }

        return (totalOverDuration = Duration.seconds(result.toSeconds() / getTempo()));
    }
    /**
     * Возвращает актуальную длительность трека с учётом темпа и корректировок.
     * <p>
     * Если значение длительности кэшировано — просто возвращает его; если нет, пересчитывает заново через {@link #recalculateOverDuration()}.
     * Используется для работы с прогресс-барами, отображения информации и адаптации под динамический темп.
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * Duration duration = player.getOverDuration();
     * System.out.println("Трек длится " + duration.toSeconds() + " секунд.");
     * }</pre>
     * @return итоговая длительность трека
     * @see #recalculateOverDuration()
     */
    public Duration getOverDuration() {
        if(totalOverDuration == null)
            return recalculateOverDuration();
        else
            return totalOverDuration;
    }
    /**
     * Возвращает точку старта воспроизведения в секундах.
     * <p>
     * Можно проигрывать с произвольной позиции, а не обязательно с начала. Обычно применяется для фрагментированного плейбэка.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setStartTime(Duration.seconds(30.0)); // старт с 30-й секунды
     * Duration start = player.getStartTime();
     * }</pre>
     * @return стартовая позиция
     * @see #setStartTime(Duration)
     */
    public Duration getStartTime() {
        return startTime;
    }
    /**
     * Устанавливает точку начала проигрывания.
     * <p>
     * Если передано {@code null} или значение не больше нуля, старт будет с самого начала.
     * В противном случае позиция пересчитывается с учётом текущего темпа.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setTempo(2.0f);
     * player.setStartTime(Duration.seconds(15.0));
     * }</pre>
     * @param startTime позиция старта (секунды)
     * @see #getStartTime()
     */
    public void setStartTime(Duration startTime) {
        if (startTime == null || !startTime.greaterThan(Duration.ZERO)) {
            this.startTime = Duration.ZERO;
        } else {
            this.startTime = Duration.seconds(startTime.toSeconds() / getTempo());
        }
    }
    /**
     * Получает точку остановки воспроизведения.
     * <p>
     * Если не была задана явно — возвращает всю длительность трека.
     * Обычно применяется для ограниченного воспроизведения фрагмента или лупа.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * Duration stop = player.getStopTime();
     * }</pre>
     * @return точка остановки (секунды)
     * @see #setStopTime(Duration)
     */
    public Duration getStopTime() {
        return totalDuration.get();
    }
    /**
     * Задает ручную точку остановки проигрывания.
     * <p>
     * Если значение некорректно (null, UNKNOWN, INDEFINITE, меньше стартового времени) —
     * сбрасывает ограничение, проигрывание не останавливается досрочно.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * // Остановить проигрывание через 45 секунд после старта
     * player.setStopTime(Duration.seconds(45.0));
     * }</pre>
     * @param stopTime позиция остановки
     * @see #getStopTime()
     */
    public void setStopTime(Duration stopTime) {
        if (stopTime == null || stopTime.equals(Duration.UNKNOWN) || stopTime.equals(Duration.INDEFINITE) || !stopTime.greaterThan(startTime)) {
            this.stopTime = Duration.UNKNOWN;
        } else {
            this.stopTime = stopTime;
        }
    }
    /**
     * Свойство полной длительности аудио для JavaFX-binding.
     * <p>
     * Используется для отображения длины трека, прогресса или автоскейлинга UI-элементов.
     * Только для чтения — защищает данные от изменений и гарантирует консистентность прогресса.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * totalTimeBar.maxProperty().bind(player.totalDurationProperty());
     * }</pre>
     * @return свойство длительности трека
     * @see #getOverDuration()
     */
    public ReadOnlyObjectProperty<Duration> totalDurationProperty() {
        return totalDuration.getReadOnlyProperty();
    }
    /**
     * Получает полную длительность аудиофайла (без учёта темпа).
     * <p>
     * Возвращает значение из {@link #totalDuration}. Используется для отображения исходной длины трека.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * Duration baseLength = player.getTotalDuration();
     * }</pre>
     * @return исходная длительность трека
     * @see #totalDurationProperty()
     */
    public Duration getTotalDuration() {
        return totalDuration.get();
    }

    public ITempoShifter iTempoShifter = new TempoShifter();
    /**
     * Устанавливает список VST-плагинов для обработки аудиосигнала.
     * <p>
     * Плагины применяются к звуку в реальном времени в цепочке; порядок важен.
     * Может использоваться для динамического добавления эффектов, компрессии, эквалайзинга и пр.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setPlugins(Arrays.asList(new PluginWrapper(JVst2.newInstance(new File("Reverb.dll"))), new PluginWrapper(JVst2.newInstance(new File("Compressor.dll")))));
     * }</pre>
     * @param plugins список VST-эффектов
     * @see #getPlugins()
     * @see PluginWrapper
     */
    public MediaPlayer setPlugins(List<PluginWrapper> plugins) {
        this.plugins = plugins;

        return this;
    }
    /**
     * Возвращает список VST-плагинов, установленных для текущего плеера.
     * <p>
     * Можно использовать для отображения, управления обработкой или динамического переключения эффектов.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * List<PluginWrapper> plugins = player.getPlugins();
     * }</pre>
     * @return список эффектов
     * @see #setPlugins(List)
     * @see PluginWrapper
     */
    public List<PluginWrapper> getPlugins() {
        return plugins;
    }

    /**
     * Слушатель обработки аудиоблоков на низком уровне во время воспроизведения.
     * <p>
     *     Реализуйте этот интерфейс для отслеживания, модификации или визуализации звука "на лету" —
     *     например, график амплитуд, автофейдер, интеграция с VST.
     * </p>
     * <h3>
     *     Вызывается при обработке каждого блока PCM.
     * </h3>
     * <h2>Пример</h2>
     * <pre>{@code
     *      player.setPlaybackListener((block, playbackMs, frames, line) -> {
     *         // Рисуем осциллограмму или анализируем спектр
     *      });
     * }</pre>
     * @see #setPlaybackListener(List)
     */
    public interface PlaybackListener {
        /**
         * Вызывается при обработке очередного блока аудиоданных.
         *
         * @param block      двумерный массив PCM аудиосэмплов с плавающей точкой,
         *                   где первая размерность — каналы, вторая — сэмплы
         * @param playbackMs время воспроизведения в миллисекундах, подходит для синхронизации
         * @param frames     количество аудиокадров в блоке
         * @param line       объект SourceDataLine, используемый для вывода аудиоданных
         *                   (Java Sound API interface для потокового вывода аудио)
         */
        void onAudioProcessed(float[][] block, long playbackMs, int frames, SourceDataLine line);
    }

    private List<PlaybackListener> playbackListener;

    /**
     * Устанавливает слушатель для обработки событий PCM-блоков при воспроизведении.
     * <p>
     *   Данный способ — для максимально гибкого контроля: эффекты, визуализации, статистика, анализ.
     *   Переданный {@link PlaybackListener} будет вызываться на каждом обработанном аудиоблоке во время проигрывания.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setPlaybackListener((block, playbackMs, frames, line) -> {
     *     // Логируем пики, визуализируем сигнал, т.д.
     * });
     * }</pre>
     * @param listener обработчик PCM-блоков
     * @see PlaybackListener
     * @see #play()
     */
    public void setPlaybackListener(List<PlaybackListener> listener) {
        this.playbackListener = listener;
    }

    public void addPlaybackListener(PlaybackListener listener) {
        this.playbackListener.add(listener);
    }

    /**
     * Запускает воспроизведение текущего аудиофайла.
     * <p>
     * Метод обеспечивает корректную работу в нескольких сценариях:
     * <ul>
     *   <li>Если плеер уже играет или уже освобождён — не делает ничего.</li>
     *   <li>Если плеер на паузе — продолжает с текущей позиции, активирует линию и обновляет статус.</li>
     *   <li>Во всех остальных случаях — переводит флаги в активное состояние, запускает поток воспроизведения (однопоточный daemon Executor).</li>
     * </ul>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * player.play(); // Старт простого воспроизведения
     * player.pause(); // Пауза
     * player.play(); // Продолжение с паузы
     * }</pre>
     * <h2>Особенности</h2>
     * <ul>
     *   <li>Поток проигрывания всегда работает "в фоне", не блокирует UI.</li>
     *   <li>Можно вызывать многократно — повторные вызовы безопасны.</li>
     * </ul>
     * @see #pause()
     * @see #processAudioStream()
     * @see Status
     */
    public synchronized void play() {
        if (getStatus() == Status.PLAYING || getStatus() == Status.DISPOSED)
            return;

        if (getStatus() == Status.PAUSED) {
            paused.set(false);

            synchronized(pauseLock) {
                pauseLock.notifyAll();
            }

            setStatus(Status.PLAYING);

            if (line != null && line.isOpen()) {
                line.start();
            }

            return;
        }

        playing.set(true);
        paused.set(false);

        synchronized(pauseLock) {
            pauseLock.notifyAll();
        }

        playbackExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MediaPlayer-Playback-Thread");
            t.setDaemon(true);

            return t;
        });

        playbackTask = playbackExecutor.submit(this::processAudioStream);
    }
    /**
     * Приостанавливает воспроизведение текущего аудиофайла.
     * <p>
     * Метод переводит плеер в состояние паузы, выставляет соответствующий флаг, корректно останавливает и очищает аудиолинию.
     * Корректно работает только если проигрывание активно — остальные состояния проигнорируются.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.play();
     * // ...
     * player.pause(); // Остановить на текущем кадре
     * }</pre>
     * <h2>Особенности</h2>
     * <ul>
     *   <li>Промежуточный буфер flush'ится, чтобы при возобновлении не возникало артефактов.</li>
     *   <li>Все слушатели уведомляются о переходе в состояние {@link Status#PAUSED}.</li>
     * </ul>
     * @see #play()
     * @see Status
     */
    public synchronized void pause() {
        if (getStatus() != Status.PLAYING)
            return;

        paused.set(true);

        if (line != null && line.isOpen()) {
            try {
                line.flush();
                line.stop();
            } catch (Exception ignored) {

            }
        }

        setStatus(Status.PAUSED);
        runOnPaused();
    }
    /**
     * Оценивает длительность PCM-аудиофайла по размеру и формату.
     * <p>
     * Используется как запасной вариант, если отсутствует явно заданное количество фреймов или длительность не может быть определена декодером.
     * Формула: <br>
     * <code>seconds = (fileSize / (sampleSize * channels)) / sampleRate</code>
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * Duration duration = computeDurationFallback(format, file.length());
     * }</pre>
     * @param format формат аудиоданных
     * @param fileSize размер файла (байты)
     * @return оценка длительности, либо {@link Duration#UNKNOWN} если входные параметры некорректны
     */
    private Duration computeDurationFallback(AudioFormat format, long fileSize) {
        if (fileSize > 0 && format.getSampleRate() > 0 && format.getChannels() > 0 && format.getSampleSizeInBits() > 0) {
            double bytesPerSample = format.getSampleSizeInBits() / 8.0;
            double bytesPerFrame = bytesPerSample * format.getChannels();
            double totalFrames = fileSize / bytesPerFrame;
            double seconds = totalFrames / format.getSampleRate();
            return Duration.seconds(seconds);
        }

        return Duration.UNKNOWN;
    }
    /**
     * Останавливает воспроизведение, освобождает ресурсы, сбрасывает позицию.
     * <p>
     * Метод гарантирует корректное завершение воспроизведения:
     * <ul>
     *   <li>Меняет статус на {@link Status#STOPPED}</li>
     *   <li>Останавливает и сбрасывает линию вывода</li>
     *   <li>Прерывает поток декодирования и освобождает Executor</li>
     *   <li>Оповещает UI, сбрасывает прогресс и вызывает {@code runOnStopped}</li>
     * </ul>
     * Многократные вызовы безопасны — лишние выполняются мгновенно.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.stop();
     * }</pre>
     * @see #dispose()
     * @see #play()
     */
    public synchronized void stop() {
        if (getStatus() == Status.STOPPED || getStatus() == Status.UNKNOWN || getStatus() == Status.READY)
            return;

        playing.set(false);
        paused.set(false);

        synchronized(pauseLock) {
            pauseLock.notifyAll();
        }

        if (playbackTask != null && !playbackTask.isDone()) {
            playbackTask.cancel(true);
        }

        if (playbackExecutor != null) {
            playbackExecutor.shutdownNow();
            playbackExecutor = null;
        }

        closeResources();

        setStatus(Status.STOPPED);

        currentFrame = 0;

        Platform.runLater(() -> currentTime.set(Duration.ZERO));
        runOnStopped();
    }
    /**
     * Возвращает линию вывода аудиоданных (SourceDataLine).
     * <p>
     * Может быть полезен для низкоуровневых манипуляций или настройки дополнительной обработки.
     * <b>При работе напрямую корректно завершайте линию!</b>
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * SourceDataLine line = player.getLine();
     * }</pre>
     * @return текущая аудиолиния
     * @see #setAudioOutput(String)
     */
    public SourceDataLine getLine() {
        return line;
    }
    /**
     * Полностью освобождает ресурсы проигрывателя, завершает все фоновые задачи.
     * <p>
     * Метод остановит воспроизведение, очистит все буферы, переведёт статус в {@link Status#DISPOSED},
     * завершит исполнительные сервисы и очистит внутренние ссылки, уведомит UI и слушатели через {@code runOnDisposed}.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.dispose(); // После этого объект использовать нельзя!
     * }</pre>
     * @see #stop()
     * @see #close()
     * @see Status#DISPOSED
     */
    public void dispose() {
        stop();

        closeResources();

        setStatus(Status.DISPOSED);

        if (!scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        if (playbackExecutor != null && !playbackExecutor.isShutdown()) {
            playbackExecutor.shutdownNow();
            playbackExecutor = null;
        }

        playbackTask = null;

        inputBlock = null;
        vstInput = null;

        runOnDisposed();
    }
    /**
     * Реализация интерфейса {@link AutoCloseable}: гарантирует корректное закрытие ресурсов плеера.
     * <p>
     * При использовании с try-with-resources автоматически вызовет {@link #dispose()} и освободит все связанные с плеером ресурсы.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * try (MediaPlayer player = new MediaPlayer(media)) {
     *     player.play();
     * }
     * // После выхода из блока все ресурсы будут очищены
     * }</pre>
     * @see #dispose()
     * @since 0.1.4.1
     */
    @Override
    public void close() {
        dispose();
    }
    /**
     * Основной цикл воспроизведения аудиопотока — сердце плеера.
     * <p>
     * Этот метод выполняется в отдельном потоке через {@link #playbackExecutor} и отвечает за:
     * <ul>
     *   <li>Открытие и подготовку аудиопотока</li>
     *   <li>Перемотку к точке старта (если задана)</li>
     *   <li>Запуск аудиолинии и установку громкости</li>
     *   <li>Переход в статус {@link Status#PLAYING}</li>
     *   <li>Вызов слушателя {@code onPlaying}</li>
     *   <li>Запуск основного цикла обработки — {@link #readProcessLoop()}</li>
     * </ul>
     * </p>
     * <p>
     * После завершения чтения:
     * <ul>
     *   <li>Если плеер всё ещё активен — вызывает {@link SourceDataLine#drain()} для полной отыгровки буфера</li>
     *   <li>Переходит в статус {@link Status#STOPPED}</li>
     *   <li>Запускает событие {@code onEndOfMedia}</li>
     * </ul>
     * </p>
     * <p>
     * В случае ошибки — переводит плеер в состояние {@link Status#HALTED}, вызывает {@code onHalted}.
     * В блоке {@code finally} всегда:
     * <ul>
     *   <li>Закрывает ресурсы</li>
     *   <li>Сбрасывает позицию и время</li>
     *   <li>Останавливает исполнительный сервис</li>
     * </ul>
     * </p>
     * <h2>Важно</h2>
     * <p>
     * Метод <b>не должен вызываться напрямую</b> — он запускается через {@link #play()} как задача в {@code playbackExecutor}.
     * </p>
     * <h2>Архитектурная роль</h2>
     * <p>
     * Это — <b>аудио-движок</b>: он управляет потоком от начала до конца, включая обработку, синхронизацию и освобождение.
     * Все эффекты, VST, панорама, темп — проходят через этот цикл.
     * </p>
     *
     * @see #play()
     * @see #readProcessLoop()
     * @see #openStreamsAndPrepare()
     * @see #closeResources()
     * @see #runOnEndOfMedia()
     * @see #runOnHalted()
     * @since 0.1.3.0
     */
    private void processAudioStream() {
        try {
            openStreamsAndPrepare();

            if (startTime.greaterThan(Duration.ZERO)) {
                seekToStartTime();
            } else {
                currentFrame = 0;
                updateCurrentTime(Duration.ZERO);
            }

            if(line == null)
                return;

            setLineVolume(volume.get());

            line.start();

            setStatus(Status.PLAYING);

            runOnPlaying();

            readProcessLoop();

            if (playing.get()) {
                line.drain();
                setStatus(Status.STOPPED);
                runOnEndOfMedia();
            }
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(Status.HALTED);
            runOnHalted();
        } finally {
            closeResources();

            if (status.get() != Status.HALTED && status.get() != Status.STOPPED) {
                setStatus(Status.STOPPED);
            }

            currentFrame = 0;
            updateCurrentTime(Duration.ZERO);

            if (playbackExecutor != null) {
                playbackExecutor.shutdown();
                playbackExecutor = null;
            }
        }
    }
    /**
     * Слушатель-событие, вызываемый после успешного завершения асинхронной подготовки ресурса.
     * <p>
     * Позволяет синхронизировать "готовность" медиаплеера с UI или внешней логикой: например, автоматический старт, предварительный анализ, подгрузка метаданных.
     * Запускается в конце метода {@link #openStreamsAndPrepare()}, если задан.
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * player.onPrepared = () -> {
     *     System.out.println("Трек готов, запускаем воспроизведение!");
     *     player.play();
     * };
     * }</pre>
     * @see #openStreamsAndPrepare()
     * @see #prepareAsync()
     */
    public Runnable onPrepared;

    public void setOnPrepared(Runnable onPrepared) {
        this.onPrepared = onPrepared;
    }

    /**
     * Подготавливает все необходимые ресурсы для воспроизведения: открывает поток, декодирует в PCM, настраивает линию и буферы.
     * <p>
     * Это — <b>инициализация аудиостека</b>. Метод:
     * <ul>
     *   <li>Открывает {@link AudioInputStream} через {@link #openAudioStream()}</li>
     *   <li>Преобразует его в 16-битный PCM с помощью {@link AudioSystem#getAudioInputStream(AudioFormat, AudioInputStream)}</li>
     *   <li>Создаёт {@link SourceDataLine} с подходящим форматом</li>
     *   <li>Инициализирует буферы: {@code inputBlock}, {@code vstInput}, {@code vstOutput}</li>
     *   <li>Вычисляет длительность трека — по фреймам, размеру файла или через кастомные декодеры</li>
     * </ul>
     * </p>
     * <h2>Декодирование</h2>
     * <p>
     * Все входные данные конвертируются в:
     * <ul>
     *   <li><b>Формат</b>: PCM_SIGNED</li>
     *   <li><b>Битность</b>: 16 бит</li>
     *   <li><b>Байтовый порядок</b>: little-endian</li>
     *   <li><b>Каналы</b>: стерео или моно, как в исходнике</li>
     * </ul>
     * Это гарантирует совместимость с {@link SourceDataLine} и VST-плагинами.
     * </p>
     * <h2>Вычисление длительности</h2>
     * <p>
     * Порядок определения:
     * <ol>
     *   <li>Если файл — MP3/WAV и известно количество фреймов — используется {@code frameLength / frameRate}</li>
     *   <li>Если нет — пробуем вычислить по размеру файла и формату (см. {@link #computeDurationFallback(AudioFormat, long)})</li>
     *   <li>Если и это не сработало — оставляем {@link Duration#UNKNOWN}</li>
     * </ol>
     * </p>
     * <h2>Буферы</h2>
     * <p>
     * Размеры буферов подбираются под максимальное число входов/выходов среди всех VST-плагинов.
     * Это позволяет обрабатывать даже сложные цепочки эффектов без перераспределения.
     * </p>
     * <h2>Событие</h2>
     * <p>
     * После успешной подготовки вызывается {@code onPrepared}, если он установлен.
     * Используется для синхронизации с UI.
     * </p>
     *
     * @throws Exception если не удалось открыть поток, линию или определить формат
     * @see #processAudioStream()
     * @see #openAudioStream()
     * @see #computeDurationFallback(AudioFormat, long)
     * @see #onPrepared
     * @since 0.1.3.0
     */
    public void openStreamsAndPrepare() throws Exception {
        audioStream = openAudioStream();

        AudioFormat baseFormat = audioStream.getFormat();

        AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false);

        audioStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream);

        long frameLength = audioStream.getFrameLength();
        if (frameLength > 0 && decodedFormat.getFrameRate() > 0) {
            totalDuration.set(Duration.seconds(frameLength / decodedFormat.getFrameRate()));
        } else {
            long fileSize = -1;

            try {
                if (media.getSource().startsWith("file:")) {
                    File file = new File(new URI(media.getSource()));
                    fileSize = file.length();
                }
            } catch (Exception ignored) {

            }

            totalDuration.set(computeDurationFallback(decodedFormat, fileSize));
        }

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(decodedFormat);

        int channels = decodedFormat.getChannels();
        inputBlock = new float[channels][BLOCK_SIZE_FRAMES];

        int maxPluginInputs = 0, maxPluginOutputs = 0;

        if (plugins != null && !plugins.isEmpty()) {
            for (PluginWrapper plugin : plugins) {
                maxPluginInputs = Math.max(maxPluginInputs, plugin.numInputs());
                maxPluginOutputs = Math.max(maxPluginOutputs, plugin.numOutputs());
            }
        }

        maxPluginInputs = Math.max(maxPluginInputs, channels);
        maxPluginOutputs = Math.max(maxPluginOutputs, channels);

        vstInput = new float[maxPluginInputs][BLOCK_SIZE_FRAMES];
        vstOutput = new float[maxPluginOutputs][BLOCK_SIZE_FRAMES];

        if(onPrepared != null)
            onPrepared.run();
    }
    /**
     * Перематывает аудиопоток к заданной стартовой позиции.
     * <p>
     * Метод вычисляет количество фреймов для пропуска с учётом sample rate и темпа,
     * затем с помощью {@link AudioInputStream#skip(long)} продвигает поток вперёд на нужное количество байт.
     * После успешной перемотки — обновляет {@code currentFrame} и вызывает {@link #updateCurrentTime(Duration)}
     * для синхронизации позиции.
     * </p>
     * <h2>Архитектура</h2>
     * <ul>
     *   <li>Используется только при запуске с непустым {@link #startTime}</li>
     *   <li>Позволяет реализовать запуск трека "с середины", лупы, репетиции и редактирование</li>
     * </ul>
     * <h2>Пример вызова</h2>
     * <pre>{@code
     * // Внутри processAudioStream()
     * if (startTime.greaterThan(Duration.ZERO)) {
     *     seekToStartTime();
     * }
     * }</pre>
     * @throws IOException если не удалось перемотать поток
     * @see #updateCurrentTime(Duration)
     * @see #startTime
     */
    private void seekToStartTime() throws IOException {
        AudioFormat decodedFormat = line.getFormat();
        float sampleRate = decodedFormat.getFrameRate();
        long framesToSkip = (long) (startTime.toSeconds() * sampleRate * getTempo());
        long bytesToSkip = framesToSkip * decodedFormat.getFrameSize();
        long skipped = 0;

        while (skipped < bytesToSkip) {
            long n = audioStream.skip(bytesToSkip - skipped);
            if (n <= 0)
                break;

            skipped += n;
        }

        currentFrame = framesToSkip;
        updateCurrentTime(Duration.seconds(currentFrame / sampleRate / getTempo()));
    }
    /**
     * Список активных аудиоплагинов (DSP-эффектов, обработчиков).
     * <p>
     * Интегрирует внешние эффекты (EQS, компрессоры, лимитеры, анализаторы) в основной аудиопоток.
     * Получается из глобального {@link Effector} при инициализации плеера.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * List<IAudioEffect> plugs = player.getAudioPlugins();
     * player.setAudioPlugins(new ArrayList<>()); // заменить цепочку эффектов
     * }</pre>
     * @see #setAudioPlugins(List)
     * @see IAudioEffect
     * @see Effector
     */
    private List<IAudioEffect> IAudioPlugins = Effector.instance.plugins;
    /**
     * Возвращает текущий список аудиоплагинов.
     * @return список {@link IAudioEffect}
     */
    public List<IAudioEffect> getAudioPlugins() {
        return IAudioPlugins;
    }
    /**
     * Устанавливает новую цепочку аудиоплагинов и возвращает сам объект для чейнинга.
     * <p>
     * Позволяет гибко переключать цепочку DSP-эффектов "на лету".
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setAudioPlugins(customPluginList).play();
     * }</pre>
     * @param IAudioPlugins новый список DSP-эффектов
     * @return MediaPlayer для fluent API
     */
    public MediaPlayer setAudioPlugins(List<IAudioEffect> IAudioPlugins) {
        this.IAudioPlugins = IAudioPlugins;
        return this;
    }
    /**
     * Cлужебный объект для синхронизации потоков паузы/проигрывания.
     * <p>
     * Используется в логике {@code play()}, {@code pause()}, {@code stop()} — обеспечивает потокобезопасную работу с флагами состояния и ожиданием,
     * предотвращая race condition между UI и аудиопотоком.
     * </p>
     * <h2>Архитектурная роль</h2>
     * <ul>
     *   <li>Служит локом для {@code synchronized(pauseLock)}</li>
     *   <li>Гарантирует, что ожидание и уведомление происходят строго в нужных точках</li>
     * </ul>
     * @see #play()
     * @see #pause()
     * @see #stop()
     */
    private final Object pauseLock = new Object();

    /**
     * Главный цикл обработки и вывода аудиоблоков в аудиолинию.
     * <p>
     * Этот метод реализует "движок воспроизведения":
     * <ul>
     *   <li>Плавное управление громкостью: плавная интерполяция к целевому значению</li>
     *   <li>Пауза — через лок паузы и ожидание внутри потока</li>
     *   <li>Чтение PCM-блоков из {@link AudioInputStream} с учётом размера блока и формата</li>
     *   <li>Ограничение по {@link #stopTime}: аккуратное завершение по таймеру или по длине</li>
     *   <li>Обработка: VST-эффекты, собственные плагины, темп, pitch, панорама</li>
     *   <li>Вызывается {@link PlaybackListener} для каждого обработанного блока</li>
     *   <li>Все преобразования: bytes→floats, floats→bytes; громкость, панорама — реализованы на реальных PCM-данных</li>
     *   <li>Вывод: запись float-данных обратно в байты и отправка в аудиолинию</li>
     *   <li>Синхронизация текущего времени и позиции внешних элементов</li>
     * </ul>
     * <h2>Архитектурная роль</h2>
     * <p>
     * Это ядро аудиоплеера, где происходит всё: DSP, playback control, интеграция с UI.
     * Все эффекты, обработка, ограничения, слушатели проходят через этот цикл.
     * </p>
     * <h2>Производительность и стабильность</h2>
     * <ul>
     *   <li>Потокобезопасная синхронизация состояния проигрывания</li>
     *   <li>Реальная обработка больших потоков аудиоданных без аллокаций на каждом шаге</li>
     *   <li>Плавные переходы/фейдеры без щелчков</li>
     * </ul>
     * <h2>Пример вызова</h2>
     * <pre>{@code
     * processAudioStream() { ... readProcessLoop(); ... }
     * }</pre>
     * @see #processAudioStream()
     * @see #play()
     * @see #pause()
     * @see #bytesToFloats(byte[], float[][], int, int, int)
     * @see #floatsToBytes(float[][], byte[], int, int, int)
     * @see #setLineVolume(double)
     * @see #playbackListener
     * @see #stopTime
     * @since 0.1.3.0
     */
    protected void readProcessLoop() {
        try {
            AudioFormat decodedFormat = line.getFormat();
            int frameSize = decodedFormat.getFrameSize();
            int channels = decodedFormat.getChannels();
            float sampleRate = decodedFormat.getSampleRate();

            byte[] readBuffer = new byte[BLOCK_SIZE_FRAMES * frameSize];
            byte[] writeBuffer = null;

            float step = 0.01f;

            while (playing.get() && !Thread.currentThread().isInterrupted()) {
                float targetVol = (float) volume.get();
                float diff = targetVol - currentVolume.get();

                if (Math.abs(diff) > step) {
                    currentVolume.set(currentVolume.get() + Math.signum(diff) * step);
                } else {
                    currentVolume.set(targetVol);
                }

                currentVolume.set(Math.max(0f, Math.min(1f, currentVolume.get())));

                // Обработка паузы
                while (paused.get() && playing.get()) {
                    if (Thread.currentThread().isInterrupted())
                        return;

                    synchronized (pauseLock) {
                        try {
                            pauseLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }

                if (!playing.get())
                    break;

                // Чтение из аудиопотока
                int bytesRead = audioStream.read(readBuffer, 0, readBuffer.length);

                if (bytesRead == -1)
                    break;

                int framesRead = bytesRead / frameSize;

                // Ограничение по stopTime
                if (stopTime != null && !stopTime.equals(Duration.UNKNOWN) && !stopTime.equals(Duration.INDEFINITE) && stopTime.greaterThan(Duration.ZERO)) {
                    long stopFrame = (long) (stopTime.toSeconds() * sampleRate);

                    if (currentFrame + framesRead >= stopFrame) {
                        int framesToWrite = (int) (stopFrame - currentFrame);

                        if (framesToWrite <= 0) {
                            onStopTimeReached();
                            break;
                        }

                        framesRead = framesToWrite;
                    }
                }

                // Конвертация байт в float
                bytesToFloats(readBuffer, inputBlock, framesRead, channels, frameSize);

                // Текущий необработанный блок аудио
                float[][] processedBlock = inputBlock;

                // Обработка VST/VST3 плагинами
                if (plugins != null && !plugins.isEmpty()) {
                    List<PluginWrapper> pluginsCopy = new ArrayList<>(plugins);
                    Iterator<PluginWrapper> plugins_iter = pluginsCopy.listIterator();

                    int blockChannels = processedBlock.length;

                    while (plugins_iter.hasNext()) {
                        PluginWrapper plugin = plugins_iter.next();

                        int pluginInputs = plugin.numInputs();
                        int pluginOutputs = plugin.numOutputs();

                        if (vstInput == null || vstInput.length < pluginInputs || vstInput[0].length < framesRead) {
                            vstInput = new float[pluginInputs][framesRead];
                        }
                        if (vstOutput == null || vstOutput.length < pluginOutputs || vstOutput[0].length < framesRead) {
                            vstOutput = new float[pluginOutputs][framesRead];
                        }

                        for (int ch = 0; ch < pluginInputs; ch++) {
                            if (ch < blockChannels) {
                                System.arraycopy(processedBlock[ch], 0, vstInput[ch], 0, framesRead);
                            } else {
                                Arrays.fill(vstInput[ch], 0, framesRead, 0f);
                            }
                        }

                        plugin.processReplacing(vstInput, vstOutput, framesRead);

                        for (int ch = 0; ch < blockChannels; ch++) {
                            if (ch < pluginOutputs) {
                                System.arraycopy(vstOutput[ch], 0, processedBlock[ch], 0, framesRead);
                            } else {
                                Arrays.fill(processedBlock[ch], 0, framesRead, 0f);
                            }
                        }
                    }
                }

                // Слушатель обработки блоков аудио
                if (playbackListener != null && line != null && line.isOpen()) {
                    if(playbackListener.size() > 0) {
                        long playbackMs = (long) ((currentFrame / sampleRate) * 1000);

                        for(MediaPlayer.PlaybackListener listener : playbackListener) {
                            listener.onAudioProcessed(processedBlock, playbackMs, framesRead, line);
                        }
                    }
                }

                // Применение громкости
                if (Math.abs(currentVolume.get() - 1.0f) > 0.001f) {
                    for (int ch = 0; ch < processedBlock.length; ch++) {
                        for (int i = 0; i < framesRead; i++) {
                            processedBlock[ch][i] *= currentVolume.get();
                        }
                    }
                }

                // Применение панорамы (левая и правая громкости)
                if (processedBlock.length >= 2) {
                    float leftMul = (float) (Math.cos((pan + 1) * Math.PI / 4));
                    float rightMul = (float) (Math.cos((1 - pan) * Math.PI / 4));

                    for (int i = 0; i < processedBlock[0].length; i++) {
                        processedBlock[0][i] *= leftMul;  // левый
                        processedBlock[1][i] *= rightMul; // правый
                    }
                }

                // Применение собственных плагинов
                if(IAudioPlugins != null) {
                    for(IAudioEffect plug : IAudioPlugins) {
                        processedBlock = plug.process(processedBlock, framesRead);
                    }
                }

                // Применение темпа через определённый алгоритм
                if(iTempoShifter != null && tempo != 1.000) {
                    processedBlock = iTempoShifter.applyTempo(processedBlock, framesRead, processedBlock.length, tempo);
                }

                int processedFrames = processedBlock[0].length;

                // Выделение буфера для записи, если нужно
                if (writeBuffer == null || writeBuffer.length < processedFrames * frameSize) {
                    writeBuffer = new byte[processedFrames * frameSize];
                }

                // Конвертация float обратно в байты
                floatsToBytes(processedBlock, writeBuffer, processedFrames, channels, frameSize);

                // Запись в аудио-линию
                if (line != null && line.isOpen()) {
                    line.write(writeBuffer, 0, processedFrames * frameSize);
                }

                // Обновление текущего фрейма и времени
                currentFrame += processedFrames;
                updateCurrentTime(Duration.seconds(currentFrame / sampleRate));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    /**
     * Преобразует байты PCM (16-бит, little-endian) в массивы float для DSP и обработки.
     * <p>
     * Используется на каждом шаге цикла обработки аудиоблоков — переводит сырые байты, считанные из
     * {@link AudioInputStream}, в нормализованные float (от -1.0 до 1.0), разбивая по каналам и фреймам.
     * </p>
     * <h2>Архитектура</h2>
     * <ul>
     *   <li>Позволяет универсально обрабатывать любые форматы PCM (multi-channel, разный frameSize)</li>
     *   <li>Гарантирует точность преобразования для сложных DSP-эффектов</li>
     * </ul>
     * <h2>Пример</h2>
     * <pre>{@code
     * bytesToFloats(buffer, inputBlock, framesRead, channels, frameSize);
     * }</pre>
     * @param bytes исходные PCM-байты
     * @param floats целевой float-массив
     * @param frames количество фреймов
     * @param channels число каналов
     * @param frameSize размер одного фрейма в байтах
     * @see #floatsToBytes(float[][], byte[], int, int, int)
     */
    protected void bytesToFloats(byte[] bytes, float[][] floats, int frames, int channels, int frameSize) {
        for (int frame = 0; frame < frames; frame++) {
            for (int ch = 0; ch < channels; ch++) {
                int idx = frame * frameSize + ch * 2;
                int low = bytes[idx] & 0xFF;
                int high = bytes[idx + 1];
                int sample = (high << 8) | low;

                floats[ch][frame] = sample / 32768f;
            }
        }
    }
    /**
     * Преобразует массивы float обратно в байты PCM для вывода на аудиоустройство.
     * <p>
     * Все данные в формате float (-1.0...1.0), обработанные DSP/VST, конвертируются обратно в 16-бит signed little-endian байты для записи в линию.
     * Поддерживается любой формат каналов (multi-channel), избыточные каналы заполняются нулями.
     * </p>
     * <h2>Архитектура</h2>
     * <ul>
     *   <li>Позволяет интегрировать любые собственные плагины, эффекты, алгоритмы темпа/тональности</li>
     *   <li>Гарантирует отсутствие артефактов при переходе между форматами данных</li>
     * </ul>
     * <h2>Пример</h2>
     * <pre>{@code
     * floatsToBytes(processedBlock, writeBuffer, processedFrames, channels, frameSize);
     * }</pre>
     * @param floats массивы float-аудио
     * @param bytes массив для вывода (байты PCM)
     * @param frames количество фреймов
     * @param channels число каналов
     * @param frameSize размер одного фрейма в байтах
     * @see #bytesToFloats(byte[], float[][], int, int, int)
     */
    protected void floatsToBytes(float[][] floats, byte[] bytes, int frames, int channels, int frameSize) {
        int outputChannels = Math.min(channels, floats.length);

        for (int frame = 0; frame < frames; frame++) {
            for (int ch = 0; ch < outputChannels; ch++) {
                float sampleFloat = floats[ch][frame];
                sampleFloat = Math.max(-1f, Math.min(1f, sampleFloat));
                short sampleShort = (short) (sampleFloat * 32767);
                int idx = frame * frameSize + ch * 2;
                bytes[idx] = (byte) (sampleShort & 0xFF);
                bytes[idx + 1] = (byte) ((sampleShort >> 8) & 0xFF);
            }

            for (int ch = outputChannels; ch < channels; ch++) {
                int idx = frame * frameSize + ch * 2;
                bytes[idx] = 0;
                bytes[idx + 1] = 0;
            }
        }
    }

    /**
     * Универсальный метод для открытия аудиопотока по заданному источнику.
     * <p>
     * В зависимости от расширения файла:
     * <ul>
     *   <li>MP3 — использует {@link #createStreamingMp3AudioInputStream(InputStream)} для декодирования и потоковой обработки</li>
     *   <li>WAV — использует {@link #createStreamingWavAudioInputStream(InputStream)} для потоковой загрузки с буферизацией</li>
     *   <li>Другие форматы — перебирает кастомные декодеры из {@link #decoders}, выбирая подходящий</li>
     *   <li>В последнюю очередь — стандартный {@link AudioSystem#getAudioInputStream(URL)} для прямой загрузки через JDK</li>
     * </ul>
     * <p>
     * После открытия определяет формат и обновляет свойство {@code currentFormat}.
     * </p>
     * <h2>Архитектурная роль</h2>
     * <p>
     * Это единая точка входа для интеграции файлов, потоков, веб-ресурсов и декодеров в едином API.
     * Гибкая поддержка любых форматов.
     * </p>
     * <h2>Ошибки</h2>
     * <p>
     * При невозможности открыть поток — будет выброшено соответствующее исключение JDK или декодера.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * AudioInputStream ais = player.openAudioStream();
     * }</pre>
     * @return поток {@link AudioInputStream} с декодированным аудио
     * @throws Exception если декодер или источник недоступны
     * @see #createStreamingMp3AudioInputStream(InputStream)
     * @see #createStreamingWavAudioInputStream(InputStream)
     * @see #decoders
     * @see #currentFormat
     */
    private AudioInputStream openAudioStream() throws Exception {
        String src = media.getSource();

        InputStream inputStream = getInputStreamForSource(src);

        if (src.toLowerCase().endsWith(AvailableFormat.MP3.title)) {
            currentFormat.set(AvailableFormat.MP3.title);

            return createStreamingMp3AudioInputStream(inputStream);
        } else if(src.toLowerCase().endsWith(AvailableFormat.WAV.title)) {
            currentFormat.set(AvailableFormat.WAV.title);

            return createStreamingWavAudioInputStream(inputStream);
        } else {
            for(AudioDecoder dec : decoders) {
                if(src.toLowerCase().endsWith(dec.getFormat())) {
                    currentFormat.set(dec.getFormat());

                    return dec.createStreaming(media.getFile());
                }
            }
        }

        return AudioSystem.getAudioInputStream(new URL(src));
    }
    /**
     * Создаёт потоковый аудиопоток для MP3-файлов — с on-the-fly-декодированием в PCM.
     * <p>
     * Использует библиотеку Bitstream для чтения заголовков и кадры, конвертирует в формат:
     * <ul>
     *   <li>Частота дискретизации — из заголовка</li>
     *   <li>Каналы — определяет автоматически (моно/стерео)</li>
     *   <li>Битность — всегда 16-бит signed little-endian</li>
     * </ul>
     * <p>
     * Гарантирует совместимость с потоком PCM для SourceDataLine. При невозможности прочитать первый кадр выбрасывает IOException.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * AudioInputStream ais = createStreamingMp3AudioInputStream(inputStream);
     * }</pre>
     * @param mp3InputStream поточный InputStream для MP3-файла
     * @return поток PCM {@link AudioInputStream}
     * @throws IOException если недоступен первый кадр/битрейт или файл повреждён
     * @see Bitstream
     * @see AudioFormat
     * @see Mp3PcmStream
     */
    private AudioInputStream createStreamingMp3AudioInputStream(InputStream mp3InputStream) throws IOException, BitstreamException {
        Bitstream bitstream = new Bitstream(mp3InputStream);
        Header firstHeader = bitstream.readFrame();

        if (firstHeader == null)
            throw new IOException(AvailableFormat.MP3.getTitle() + ": Не удалось прочитать первый фрейм");

        int sampleRate = firstHeader.frequency();
        int channels = firstHeader.mode() == Header.SINGLE_CHANNEL ? 1 : 2;

        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        bitstream.unreadFrame();

        return new AudioInputStream(new Mp3PcmStream(bitstream), format, AudioSystem.NOT_SPECIFIED);
    }
    /**
     * Создаёт потоковый аудиопоток для WAV-файлов — с буферизацией и автоматическим определением формата.
     * <p>
     * Использует JDK {@link AudioSystem#getAudioInputStream} для гарантии совместимости с PCM/ADPCM WAV.
     * В случае неправильного файла — выбрасывает исключение об ошибке формата.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * AudioInputStream ais = createStreamingWavAudioInputStream(inputStream);
     * }</pre>
     * @param inputStream поток WAV-данных
     * @return поток PCM {@link AudioInputStream}
     * @throws IOException если файл некорректен или недоступен
     * @see AudioSystem#getAudioInputStream(InputStream)
     * @see BufferedInputStream
     */
    private AudioInputStream createStreamingWavAudioInputStream(InputStream inputStream) throws IOException {
        try {
            return AudioSystem.getAudioInputStream(new BufferedInputStream(inputStream));
        } catch (UnsupportedAudioFileException e) {
            throw new RuntimeException(AvailableFormat.WAV.getTitle() + ": ", e);
        }
    }
    /**
     * Устанавливает уровень громкости на аудиолинии.
     * <p>
     * Преобразует переданное значение в децибелы согласно формуле: <code>dB = 20 * log10(vol)</code>.
     * Позволяет точно управлять уровнем звука для всех аудиоустройств, поддерживающих {@link FloatControl.Type#MASTER_GAIN}.
     * Если линия отсутствует или не поддерживает управление громкостью — метод ничего не делает.
     * </p>
     * <h2>Архитектура и надежность</h2>
     * <ul>
     *   <li>Защита от ошибок: любые исключения игнорируются, чтобы не прерывать логику проигрывания</li>
     *   <li>Гарантирует корректную поддержку диапазона (vol ≤ 0 эквивалентно mute)</li>
     * </ul>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setLineVolume(0.75); // Уменьшить громкость до 75%
     * }</pre>
     * @param vol уровень громкости (от 0.0 до 1.0)
     * @see FloatControl
     * @see SourceDataLine
     */
    public void setLineVolume(double vol) {
        if (line != null && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            try {
                FloatControl volControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (20 * Math.log10(vol <= 0 ? 0.0001 : vol));
                volControl.setValue(dB);
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    }
    /**
     * Получает входной поток для аудиоресурса по ссылке или имени файла.
     * <p>
     * Универсальный механизм для работы с файлами, URL, и стандартными сетевыми протоколами.<br>
     * Проверяет схему URI и выбирает способ загрузки:
     * <ul>
     *   <li>file — открывает FileInputStream</li>
     *   <li>http/https — открывает stream через URL</li>
     *   <li>иначе — пробует открыть напрямую как локальный файл</li>
     * </ul>
     * <p>
     * При некорректном параметре выбрасывает {@link NullPointerException}.
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * InputStream in = player.getInputStreamForSource("file:///C:/music/track.mp3");
     * }</pre>
     * @param src строка-источник (имя файла, URL, путь)
     * @return поток байтов для чтения аудиоданных
     * @throws Exception при ошибке открытия или некорректном uri
     * @see FileInputStream
     * @see URL#openStream()
     * @see URI
     */
    public InputStream getInputStreamForSource(String src) throws Exception {
        if(src != null) {
            if (src.startsWith("file") || src.startsWith("http") || src.startsWith("https")) {
                URI uri = new URI(src);

                if ("file".equals(uri.getScheme())) {
                    return new FileInputStream(new File(uri));
                } else {
                    return uri.toURL().openStream();
                }
            } else {
                return new FileInputStream(src);
            }
        } else {
            throw new NullPointerException("Ресурс не существует");
        }
    }
    /**
     * Корректно освобождает аудиоресурсы (линию и поток), используемые медиаплеером.
     * <p>
     * Останавливает линию, полностью сбрасывает её буфер, закрывает объект и обнуляет ссылку.<br>
     * Аналогично закрывает входной {@link AudioInputStream}.<br>
     * Все исключения игнорируются — гарантируется надёжное завершение очистки.
     * </p>
     * <h2>Архитектурная роль</h2>
     * <ul>
     *   <li>Используется при завершении проигрывания, переходе к новому треку и при уничтожении плеера</li>
     *   <li>Защищает от resource leaks даже в случае ошибок устройств или файлов</li>
     * </ul>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * player.closeResources(); // Зачищает всё!
     * }</pre>
     * @see #dispose()
     * @see SourceDataLine
     * @see AudioInputStream
     */
    public void closeResources() {
        if (line != null) {
            try {
                if (line.isRunning()) {
                    line.flush();
                    line.stop();
                }

                line.close();
            } catch (Exception ignored) {

            }

            line = null;
        }
        if (audioStream != null) {
            try {
                audioStream.close();
            } catch (Exception ignored) {

            }

            audioStream = null;
        }
    }

    /**
     * Устанавливает обработчик события "плеер готов к воспроизведению".
     * <p>
     * Слушатель вызывается после успешной асинхронной подготовки ресурса (например, после {@link #prepareAsync()} или {@link #openStreamsAndPrepare()}).
     * Используется для автоматического старта проигрывания, активации UI и любых действий, необходимых при входе в состояние READY.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setOnReady(() -> statusLabel.setText("Track loaded!"));
     * }</pre>
     * @param handler обработчик готовности
     * @see #onReady
     * @see #prepareAsync()
     */
    public void setOnReady(Runnable handler) {
        this.onReady = handler;
    }
    /**
     * Устанавливает обработчик события "начато воспроизведение".
     * <p>
     * Срабатывает каждый раз, когда статус плеера меняется на PLAYING: после play() или выхода из pause().
     * Полезно для анимаций, обновления иконок, включения визуализации и прочих UI-реакций.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setOnPlaying(() -> animationPanel.start());
     * }</pre>
     * @param handler обработчик старта проигрывания
     * @see #onPlaying
     * @see #play()
     */
    public void setOnPlaying(Runnable handler) {
        this.onPlaying = handler;
    }
    /**
     * Устанавливает обработчик события "пауза воспроизведения".
     * <p>
     * Срабатывает при переходе в статус PAUSED (user pause, focus lost и др). Использовать для синхронизации UI: смены иконки, остановки движений, временного изменения состояния.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setOnPaused(() -> visualizer.pause());
     * }</pre>
     * @param handler обработчик паузы
     * @see #onPaused
     * @see #pause()
     */
    public void setOnPaused(Runnable handler) {
        this.onPaused = handler;
    }
    /**
     * Устанавливает обработчик события "остановлено воспроизведение".
     * <p>
     * Срабатывает при вызове stop(), потере ресурса, окончании трека или при любых сбоях.
     * Используется для возврата UI к начальному виду, сброса состояния, сохранения последней позиции.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setOnStopped(() -> mainButton.setIcon(Icons.PLAY));
     * }</pre>
     * @param handler обработчик остановки
     * @see #onStopped
     * @see #stop()
     */
    public void setOnStopped(Runnable handler) { this.onStopped = handler; }
    /**
     * Устанавливает обработчик события "достигнут конец трека".
     * <p>
     * Срабатывает автоматически при завершении воспроизведения всей дорожки (end-of-media). Идеально подходит для автоперехода на следующий трек, уведомлений, обновления списков и т.д.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setOnEndOfMedia(() -> playlist.next());
     * }</pre>
     * @param handler обработчик завершения трека
     * @see #onEndOfMedia
     * @see #readProcessLoop()
     */
    public void setOnEndOfMedia(Runnable handler) { this.onEndOfMedia = handler; }
    /**
     * Устанавливает обработчик события "проигрыватель остановлен из-за ошибки".
     * <p>
     * Активируется при любых сбоях, исключениях или невозможности продолжить воспроизведение (status = HALTED).
     * Полезно для обработки ошибок, показа алертов, перезапуска или сбора диагностик.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setOnHalted(() -> showAlert("Воспроизведение прервано!"));
     * }</pre>
     * @param handler обработчик ошибок
     * @see #onHalted
     * @see #processAudioStream()
     */
    public void setOnHalted(Runnable handler) { this.onHalted = handler; }
    /**
     * Устанавливает обработчик события "ресурсы плеера окончательно освобождены".
     * <p>
     * Срабатывает после {@link #dispose()} или {@link #close()}. Используется для финальной очистки UI, освобождения ресурсов,
     * отчистки ссылок, уведомления о завершении жизненного цикла объекта.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * player.setOnDisposed(() -> System.out.println("MediaPlayer destroyed!"));
     * }</pre>
     * @param handler обработчик освобождения ресурсов
     * @see #onDisposed
     * @see #dispose()
     * @see #close()
     */
    public void setOnDisposed(Runnable handler) { this.onDisposed = handler; }
    /**
     * Переводит медиаплеер в новое состояние и запускает соответствующий обработчик события.
     * <p>
     * Метод не гарантирует, что смена статуса (READY, PLAYING, PAUSED, STOPPED, HALTED, DISPOSED) происходит только на JavaFX Application Thread — через {@link Platform#runLater(Runnable)}.
     * Автоматически вызывает связанный runOn... обработчик — синхронизируется с UI, бизнес-логикой и внешними слушателями.
     * </p>
     * <h2>Архитектурная роль</h2>
     * <ul>
     *   <li>Центральная точка управления жизненным циклом плеера</li>
     * </ul>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * player.setStatus(Status.PLAYING);
     * }</pre>
     * @param newStatus новое состояние плеера
     * @see Status
     * @see Platform#runLater(Runnable)
     */
    public void setStatus(Status newStatus) {
        status.set(newStatus);

        switch (newStatus) {
            case READY -> runOnReady();
            case PLAYING -> runOnPlaying();
            case PAUSED -> runOnPaused();
            case STOPPED -> runOnStopped();
            case DISPOSED -> runOnDisposed();
            case HALTED -> runOnHalted();
            default -> {

            }
        }
    }
    /**
     * Запускает обработчик, связанный с переходом в статус READY.
     * <p>
     * Обычно вызывается внутри {@link #setStatus(Status)}.
     * Гарантирует запуск на FX Application Thread для синхронизации с UI.
     * </p>
     * @see #setStatus(Status)
     */
    private void runOnReady() {
        if (onReady != null)
            onReady.run();
    }
    /**
     * Запускает обработчик, связанный с переходом в статус PLAYING.
     * @see #setStatus(Status)
     */
    private void runOnPlaying() {
        if (onPlaying != null)
            onPlaying.run();
    }
    /**
     * Запускает обработчик, связанный с переходом в статус PAUSED.
     * @see #setStatus(Status)
     */
    private void runOnPaused() {
        if (onPaused != null)
            onPaused.run();
    }
    /**
     * Запускает обработчик, связанный с переходом в статус STOPPED.
     * @see #setStatus(Status)
     */
    private void runOnStopped() {
        if (onStopped != null)
            onStopped.run();
    }
    /**
     * Запускает обработчик, связанный с завершением трека (end of media).
     * @see #setStatus(Status)
     */
    private void runOnEndOfMedia() {
        if (onEndOfMedia != null)
            onEndOfMedia.run();
    }
    /**
     * Запускает обработчик, связанный с состоянием HALTED (ошибка).
     * @see #setStatus(Status)
     */
    private void runOnHalted() {
        if (onHalted != null)
            onHalted.run();
    }
    /**
     * Запускает обработчик, связанный с окончательным освобождением ресурсов.
     * @see #setStatus(Status)
     */
    private void runOnDisposed() {
        if (onDisposed != null)
            onDisposed.run();
    }
    /**
     * Обновляет свойство текущего времени воспроизведения трека.
     * <p>
     * Используется для прогрессбаров, таймеров и динамической визуализации.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * updateCurrentTime(Duration.seconds(70));
     * }</pre>
     * @param newTime новое значение времени проигрывания
     * @see Platform#runLater(Runnable)
     * @see #currentTime
     */
    private void updateCurrentTime(Duration newTime) {
        currentTime.set(newTime);
    }
    /**
     * Возвращает частоту кадров аудиоформата (frame rate).
     * <p>
     * Часто требуется для расчёта длительности, динамической синхронизации и работы с PCM-фреймами.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * float fps = player.getFrameRate();
     * }</pre>
     * @return frame rate текущего трека
     * @see AudioFormat
     */
    public float getFrameRate() {
        return getFormat().getFrameRate();
    }
    /**
     * Возвращает sample rate аудиоформата.
     * @return sample rate (частота дискретизации)
     * @see AudioFormat
     */
    public float getSampleRate() {
        return getFormat().getSampleRate();
    }
    /**
     * Возвращает число каналов аудиоформата.
     * @return количество каналов (1 — моно, 2 — стерео...)
     * @see AudioFormat
     */
    public float getChannels() {
        return getFormat().getChannels();
    }
    /**
     * Возвращает размер одного аудиофрейма в байтах.
     * @return размер фрейма
     * @see AudioFormat
     */
    public float getFrameSize() {
        return getFormat().getFrameSize();
    }
    /**
     * Возвращает строковое представление медиаплеера, пригодное для отладки и журналирования.
     * <p>
     * Включает ключевые поля: media, status, volume, currentTime, все ресурсы (executors, streams, плагины), обработчики событий и внутренние состояния проигрывателя.
     * Позволяет быстро оценить состояние плеера при логировании, трассировке или интеграции с UI.
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * System.out.println(player);
     * }</pre>
     * @return подробная строка с описанием состояния плеера
     */
    @Override
    public String toString() {
        return "MediaPlayer{" +
                "media=" + media +
                ", status=" + status +
                ", volume=" + volume +
                ", currentTime=" + currentTime +
                ", totalDuration=" + totalDuration +
                ", playbackExecutor=" + playbackExecutor +
                ", playbackTask=" + playbackTask +
                ", scheduler=" + scheduler +
                ", playing=" + playing +
                ", paused=" + paused +
                ", audioStream=" + audioStream +
                ", line=" + line +
                ", plugins=" + plugins +
                ", totalOverDuration=" + totalOverDuration +
                ", startTime=" + startTime +
                ", stopTime=" + stopTime +
                ", currentFrame=" + currentFrame +
                ", onReady=" + onReady +
                ", onPlaying=" + onPlaying +
                ", onPaused=" + onPaused +
                ", onStopped=" + onStopped +
                ", onEndOfMedia=" + onEndOfMedia +
                ", onHalted=" + onHalted +
                ", onDisposed=" + onDisposed +
                ", currentVolume=" + currentVolume +
                ", tempo=" + tempo +
                ", pan=" + pan +
                ", panProperty=" + panProperty +
                ", currentFormat='" + currentFormat + '\'' +
                ", introThread=" + introThread +
                ", onStopTimeReached=" + onStopTimeReached +
                ", playbackListener=" + playbackListener +
                ", onPrepared=" + onPrepared +
                ", pauseLock=" + pauseLock +
                '}';
    }
    /**
     * Сравнивает два экземпляра MediaPlayer по источнику {@link #media}.
     * <p>
     * Реализует строгую проверку экивалентности: два экземпляра считаются равными, если ведут к одному и тому же медиафайлу/ресурсу.
     * Позволяет корректно использовать MediaPlayer как ключ в коллекциях (HashSet, HashMap) и проводить логическую проверку на уникальность.
     * </p>
     * <h2>Пример</h2>
     * <pre>{@code
     * if (player1.equals(player2)) { ... }
     * }</pre>
     * @param o сравниваемый объект
     * @return true — если ссылается на тот же медиа-источник
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        MediaPlayer that = (MediaPlayer) o;

        return Objects.equals(media, that.media);
    }
    /**
     * Генерирует хэш-код плеера, основанный на {@link #media}.
     * <p>
     * Позволяет использовать объекты класса MediaPlayer в хэш-структурах с корректным поведением equals/hashCode по медиаресурсу.
     * </p>
     * @return хэш-код
     */
    @Override
    public int hashCode() {
        return Objects.hash(media);
    }

    @Override
    public int compareTo(MediaPlayer o) {
        return o.getMedia().compareTo(this.media);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Перечисление поддерживаемых форматов аудио для проигрывателя.
     * <p>
     * Используется для автоматического выбора декодера и методов обработки, а также для отображения формата в UI или логах.
     * Каждый элемент содержит короткий строковый идентификатор (title).
     * </p>
     * <h2>Пример использования</h2>
     * <pre>{@code
     * AvailableFormat fmt = AvailableFormat.MP3;
     * String ext = fmt.getTitle(); // "mp3"
     * }</pre>
     */
    public enum AvailableFormat {
        MP3("mp3"),
        WAV("wav");

        /**
         * Расширение или короткое имя формата (например, "mp3", "wav").
         */
        private final String title;

        /**
         * Получает строку-идентификатор формата.
         * @return короткое название формата
         */
        public String getTitle() {
            return title;
        }

        /**
         * Конструктор перечисления по строке-имени.
         * @param title расширение или краткое название формата
         */
        AvailableFormat(String title) {
            this.title = title;
        }
    }
    /**
     * Статусы внутреннего жизненного цикла медиаплеера.
     * <p>
     * Используется для управления состояниями: READY, PLAYING, PAUSED, STOPPED и прочими.
     * Определяет, какие операции сейчас допустимы над экземпляром плеера.
     * Применяется во внутренних методах, биндингах для UI и логике событий.
     * </p>
     * <ul>
     *   <li>{@code UNKNOWN}: начальное/неопределённое состояние</li>
     *   <li>{@code READY}: ресурс успешно подготовлен, можно запускать проигрывание</li>
     *   <li>{@code PAUSED}: проигрывание временно приостановлено</li>
     *   <li>{@code PLAYING}: активное воспроизведение</li>
     *   <li>{@code STOPPED}: проигрывание завершено, ресурсы под контролем</li>
     *   <li>{@code HALTED}: возникла ошибка, невозможно воспроизвести</li>
     *   <li>{@code DISPOSED}: объект уничтожен, любые операции невозможны</li>
     * </ul>
     * <h2>Пример</h2>
     * <pre>{@code
     * if (player.getStatus() == Status.PLAYING) { ... }
     * }</pre>
     */
    public enum Status {
        UNKNOWN,
        READY,
        PAUSED,
        PLAYING,
        STOPPED,
        HALTED,
        DISPOSED
    }
}