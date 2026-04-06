package rf.ebanina.ebanina.Player.Controllers;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Duration;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.DataTypes;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.File.Metadata.MetadataOfFile;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.Network.Info;
import rf.ebanina.Network.Translator;
import rf.ebanina.UI.Editors.Player.AudioHost;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Element.Slider.SoundSlider;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.UI.UI.Popup.PreviewPopupService;
import rf.ebanina.ebanina.KeyBindings.KeyBind;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.AudioDecoder;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlaylistController;
import rf.ebanina.ebanina.Player.Media;
import rf.ebanina.ebanina.Player.MediaPlayer;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.utils.collections.TypicalMapWrapper;
import rf.ebanina.utils.concurrency.LonelyThreadPool;
import rf.ebanina.utils.loggining.Prefix;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static rf.ebanina.File.Field.fields;
import static rf.ebanina.Network.Info.playersMap;
import static rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor.playProcessor;
import static rf.ebanina.ebanina.Player.Controllers.Playlist.PlaylistController.checkIndexOutOfBoundPlaylist;

/**
 * <h1>MediaProcessor</h1>
 * Центральный оркестратор аудио-воспроизведения — связующее звено между UI, кастомным
 * {@link MediaPlayer}, VST-хостингом, waveform-анализом и интеллектуальными функциями скипа.
 * <p>
 * {@code MediaProcessor} — это <b>мозговой центр плеера</b>, управляющий полным жизненным циклом
 * треков: от сетевого парсинга и предзагрузки до fade-анимаций громкости и анализа waveform
 * для автоматических скипов интро/дропов/питов. Полностью заменяет JavaFX MediaPlayer.
 * </p>
 * <p>
 * Класс <b>тесно интегрирован с JavaFX UI</b> — все обновления (обложки, SoundSlider, метаданные)
 * выполняются через {@link Platform#runLater(Runnable)}}. Обеспечивает <b>thread-safe переключение треков</b>
 * через {@link LonelyThreadPool} и {@code imageLock}.
 * </p>

 * <h2>Ключевые подсистемы</h2>
 * <ul>
 *   <li><b>Custom MediaPlayer:</b> {@code mediaPlayer} + 256-frame буфер для низкой задержки</li>
 *   <li><b>VST Integration:</b> автоматическая активация {@code AudioHost.vstPlugins}</li>
 *   <li><b>Waveform Engine:</b> {@code getSubsets()} → SoundSlider + скипы через {@code skipExec}</li>
 *   <li><b>Network Stack:</b> {@code trackParseAsync()} → предзагрузка (64 файла) → локальный MP3</li>
 *   <li><b>State Persistence:</b> tempo/volume/time сохраняются по трекам в кэше</li>
 *   <li><b>Fade System:</b> 60 FPS, 500ms плавные переходы громкости + интро-поддержка</li>
 * </ul>

 * <h2>Жизненный цикл трека</h2>
 * <p>Полный пайплайн воспроизведения:</p>
 * <pre>{@code
 * _track() → [LonelyThreadPool] → _track(Track) → [2s delay]
 *   ↓
 * prepareToPlay() → regenerateMediaPlayer() → [Network|Local]
 *   ↓ readState() → fadePlay(0→1, 500ms) → mediaPlayer.play()
 *   ↓
 * updateInfo() → artProcessor.initArt() + SoundSlider + UI
 * }</pre>

 * <h2>Waveform Analysis & Smart Skips</h2>
 * <p><b>Уникальная фича:</b> реальный анализ амплитудного спектра для интеллектуальных скипов:</p>
 * <ul>
 *   <li><b>{@code getSkipDropPoint()}:</b> паттерн-распознавание (prevSimilar + nextSimilar + bigDiff 5-10px)</li>
 *   <li><b>{@code getSkipIntroPoint()}:</b> первый пик амплитуды >20px</li>
 *   <li><b>{@code getSkipPitPoint()}:</b> выход из "тишины" (diff > avgDiff×2)</li>
 *   <li><b>Нормализация:</b> 32768 (PCM max) → SoundSlider.height/2 пикселей</li>
 * </ul>

 * <h2>Сетевое воспроизведение</h2>
 * <p>Двухрежимная система с автоматической оптимизацией:</p>
 * <pre>{@code
 * isPreDownload=true:  URL → [кэш 64 файла] → local MP3 → 100% метаданные
 * isPreDownload=false: URL → stream → totalDuraSec из Track (парсинг)
 * }</pre>
 * <p>Автоочистка кэша при >64 файлов. Парсинг через {@code playersMap} (VK, Spotify, etc).</p>

 * <h2>Threading & Concurrency</h2>
 * <ul>
 *   <li><b>{@code LonelyThreadPool}:</b> гарантирует 1 трек за раз (избегает race conditions)</li>
 *   <li><b>{@code skipExec}:</b> SingleThreadExecutor для асинхронных скипов</li>
 *   <li><b>{@code trackCacheIter}:</b> атомарный счетчик обложек (очистка при CACHE_SIZE)</li>
 *   <li><b>Platform.runLater():</b> 100% UI обновления безопасны</li>
 * </ul>

 * <h2>Простое использование</h2>
 * <pre>{@code
 * // Глобальный доступ
 * MediaProcessor.mediaProcessor._track();  // Следующий трек
 *
 * // Play/Pause
 * MediaProcessor.mediaProcessor.pause_play();
 *
 * // Скип интро
 * MediaProcessor.mediaProcessor.skipIntro(track.getPath());
 *
 * // Темп + визуальный эффект
 * MediaProcessor.mediaProcessor.setTempo(1.3f);
 * }</pre>

 * <h2>Конфигурация воспроизведения</h2>
 * <table>
 *   <tr><th>Параметр</th><th>Описание</th></tr>
 *   <tr><td>{@code IS_AUTO_PLAYBACK}</td><td>Автовоспроизведение при endOfMedia</td></tr>
 *   <tr><td>{@code IS_PLAYLIST_LOOP}</td><td>Циклический плейлист</td></tr>
 *   <tr><td>{@code IS_PLAY_RANDOM}</td><td>Случайный порядок</td></tr>
 * </table>

 * <h2>Особенности реализации</h2>
 * <ul>
 *   <li><b>Singleton:</b> {@code static mediaProcessor} — глобальный доступ из Root/PlayProcessor</li>
 *   <li><b>Гибридный кэш:</b> обложки (Track.CACHE_SIZE) + сетевые треки (64 файла)</li>
 *   <li><b>State Restore:</b> "last_time"/"skip_intro"/"skip_drop" из конфига</li>
 *   <li><b>Интро-поддержка:</b> отдельный WAV-файл перед треком (fade синхронизация)</li>
 *   <li><b>Перевод названий:</b> опциональный через {@code Translator.instance}</li>
 * </ul>

 * <h2>Связанные классы</h2>
 * <ul>
 *   <li>{@link MediaPlayer} — кастомный аудио-движок</li>
 *   <li>{@link AudioDecoder} — расширяемые декодеры FLAC/AAC</li>
 *   <li>{@link AudioHost} — VST2/VST3 хостинг</li>
 *   <li>{@link SoundSlider} — waveform визуализация + сэмплы</li>
 *   <li>{@link PlayProcessor} — управление плейлистом/индексами</li>
 *   <li>{@link ArtProcessor} — анимации обложек (slide/fade/scale)</li>
 * </ul>

 * <h2>Примечания</h2>
 * <ul>
 *   <li>Замена JavaFX MediaPlayer из-за проблем с VST, стримингом и seek</li>
 *   <li>Полная потокобезопасность UI через Platform.runLater()</li>
 *   <li>Автокоррекция границ плейлиста (loop/random)</li>
 * </ul>

 * @author Ebanina Std.
 * @version 1.0
 * @since 1.4.0
 * @see MediaPlayer
 * @see AudioDecoder
 * @see PlayProcessor
 * @see ArtProcessor
 * @see SoundSlider
 */
public class MediaProcessor
{
    private final Root rootImpl;

    /**
     * Глобальный singleton экземпляр процессора медиа.
     * <p>
     * <b>Инициализация:</b> происходит при загрузке класса с параметрами из конфига:
     * </p>
     * <ul>
     *   <li><code>clear_samples=true</code>: автоочистка кэша сэмплов после waveform анализа</li>
     *   <li><code>soundSlider</code>: ссылка на глобальный SoundSlider UI компонент</li>
     * </ul>
     * <p>
     * <b>Глобальный доступ:</b> используется из {@link Root}, {@link PlayProcessor},
     * {@link PlaylistController} через <code>MediaProcessor.mediaProcessor._track()</code>.
     * </p>
     * <p>
     * <b>Thread-safety:</b> защищен внутренними пулами ({@code LonelyThreadPool}, {@code skipExec}).
     * </p>
     */
    public static MediaProcessor mediaProcessor = new MediaProcessor(
            ConfigurationManager.instance.getBooleanItem("clear_samples", "true"),
            Root.rootImpl
    );
    /**
     * Конструктор процессора медиа.
     * <p>
     * <b>Параметры:</b>
     * </p>
     * <ul>
     *   <li><code>isClearSamples</code>: управление памятью waveform сэмплов
     *       ({@code soundSlider.clearSamples()})</li>
     *   <li><code>rootImpl.soundSlider</code>: UI компонент для визуализации waveform
     *       и анализа скипов</li>
     * </ul>
     * <p>
     * Инициализирует критически важные поля для последующей работы:
     * {@code isClearSamples}, {@code rootImpl.soundSlider}.
     * </p>
     *
     * @param isClearSamples флаг очистки кэша сэмплов
     */
    public MediaProcessor(boolean isClearSamples, Root root) {
        this.isClearSamples = isClearSamples;
        this.rootImpl = root;
        this.mediaParameters.put("isAutoPlayback", new SimpleBooleanProperty(false), BooleanProperty.class);
        this.mediaParameters.put("isPlaylistLoop", new SimpleBooleanProperty(false), BooleanProperty.class);
        this.mediaParameters.put("isPlayRandom", new SimpleBooleanProperty(false), BooleanProperty.class);
    }

    /**
     * Карта параметров медиаплеера, хранящая свойства JavaFX для управления состоянием плеера.
     * <p>
     * Содержит основные флаги воспроизведения:
     * </p>
     * <ul>
     *     <li>{@code "isAutoPlayback"} - автоматическое воспроизведение следующего трека</li>
     *     <li>{@code "isAutoPlaylist"} - автоматический переход к следующему плейлисту</li>
     *     <li>{@code "isPlayRandom"} - случайное воспроизведение треков</li>
     * </ul>
     * <p>
     * </pre>
     */
    public TypicalMapWrapper<String> mediaParameters = new TypicalMapWrapper<>();

    public boolean isAutoPlayback() {
        return mediaParameters.get(MediaParameters.IS_AUTO_PLAYBACK.code, BooleanProperty.class).get();
    }

    public boolean isPlaylistLoop() {
        return mediaParameters.getAuto(MediaParameters.IS_PLAYLIST_LOOP.code);
    }

    public boolean isPlayRandom() {
        return mediaParameters.getAuto(MediaParameters.IS_PLAY_RANDOM.code);
    }

    public BooleanProperty getAutoPlaybackProperty() {
        return mediaParameters.getAuto(MediaParameters.IS_AUTO_PLAYBACK.code);
    }

    public BooleanProperty getPlaylistLoopProperty() {
        return mediaParameters.getAuto(MediaParameters.IS_PLAYLIST_LOOP.code);
    }

    public BooleanProperty getPlayRandomProperty() {
        return mediaParameters.getAuto(MediaParameters.IS_PLAY_RANDOM.code);
    }

    public TypicalMapWrapper<String> getMediaParameters() {
        return mediaParameters;
    }

    /**
     * Enum коды параметров воспроизведения для {@link #mediaParameters}.
     * <p>
     * <b>Назначение:</b> ключи для доступа к {@link Property} объектам в {@code HashMap}.
     * Гарантирует типобезопасность и автодополнение в IDE.
     * </p>
     * <table>
     *   <tr><th>Параметр</th><th>Описание</th><th>По умолчанию</th></tr>
     *   <tr><td>{@code IS_AUTO_PLAYBACK}</td><td>Автовоспроизведение при endOfMedia</td><td>false</td></tr>
     *   <tr><td>{@code IS_PLAYLIST_LOOP}</td><td>Циклический переход по плейлисту</td><td>true</td></tr>
     *   <tr><td>{@code IS_PLAY_RANDOM}</td><td>Случайный порядок треков</td><td>false</td></tr>
     * </table>
     * <p><b>Использование:</b> {@code mediaParameters.get(MediaParameters.IS_PLAYLIST_LOOP.code).getValue()}</p>
     */
    public enum MediaParameters
    {
        /**
         * Автовоспроизведение следующего трека при достижении {@code endOfMedia}.
         * <p><b>Логика:</b> {@code onEndOfMedia()} → {@code prepareToPlay(nextTrack)}</p>
         * <p><b>По умолчанию:</b> {@code false}</p>
         */
        IS_AUTO_PLAYBACK("isAutoPlayback"),
        /**
         * Циклический переход по плейлисту при выходе за границы.
         * <p><b>Логика:</b></p>
         * <pre>
         * trackIter >= tracks.size() → setTrackIter(0)
         * trackIter < 0 → setTrackIter(tracks.size()-1)
         * </pre>
         * <p><b>По умолчанию:</b> {@code true}</p>
         */
        IS_PLAYLIST_LOOP("isPlaylistLoop"),
        /**
         * Случайный порядок воспроизведения треков.
         * <p><b>Логика:</b> {@code onEndOfMedia()} → {@code new Random().nextInt(0, size-1)}</p>
         * <p><b>По умолчанию:</b> {@code false}</p>
         */
        IS_PLAY_RANDOM("isPlayRandom");
        /** Строковый код для HashMap.get(). */
        public final String code;
        /**
         * Конструктор enum'а.
         *
         * @param code строковый идентификатор параметра
         */
        MediaParameters(String code) {
            this.code = code;
        }
    }
    /**
     * Размер буфера MediaPlayer в фреймах аудио.
     * <p>
     * <b>Оптимальные значения:</b>
     * </p>
     * <ul>
     *   <li><code>256</code>: баланс latency/стабильности (по умолчанию)</li>
     *   <li><code>128</code>: низкая задержка → риск underrun</li>
     *   <li><code>512</code>: высокая стабильность → задержка ~10ms</li>
     * </ul>
     * <p>
     * Конфигурируется через <code>audio_player_block_size_frames</code>.
     * Передается в {@link MediaPlayer} конструктор.
     * </p>
     * <p><b>Влияние:</b> определяет {@code SourceDataLine.write(buffer, 256)} частоту вызовов.</p>
     */
    public final int MEDIA_PLAYER_BLOCK_SIZE_FRAMES = ConfigurationManager.instance.getIntItem("audio_player_block_size_frames", "256");
    /**
     * Одинокий пул потоков для переключения треков.
     * <p>
     * <b>Назначение:</b> гарантирует <u>последовательное выполнение</u> операций
     * {@link #_track()} (один трек за раз). Избегает race conditions при быстром
     * переключении (next/down).
     * </p>
     * <p><b>Использование:</b> {@code _trackSingleAloneThread.runNewTask()}</p>
     */
    private final LonelyThreadPool _trackSingleAloneThread = new LonelyThreadPool();
    /**
     * Атомарный счетчик кэша обложек треков.
     * <p>
     * <b>Логика очистки:</b> при {@code trackCacheIter > Track.CACHE_SIZE}:
     * </p>
     * <pre>
     * for(Track t : playProcessor.getTracks()) {
     *     t.setAlbumArt(null);
     *     t.setMipmap(null);
     * }
     * trackCacheIter.set(0);
     * </pre>
     * <p>Инкремент в {@link #updateInfo(Track)} после каждой обложки.</p>
     */
    private final AtomicInteger trackCacheIter = new AtomicInteger(0);
    /**
     * SingleThreadExecutor для асинхронных скипов.
     * <p>
     * <b>Задачи:</b> {@code skipIntro()}, {@code skipPit()}, {@code skipDrop()},
     * {@code skipOutro()}.
     * </p>
     * <p><b>Преимущества:</b> не блокирует UI/MediaPlayer, очередь FIFO.</p>
     */
    private final ExecutorService skipExec = Executors.newSingleThreadExecutor();
    /**
     * Кастомный MediaPlayer (НЕ JavaFX Media!).
     * <p>
     * <b>Ключевые отличия от JavaFX:</b>
     * </p>
     * <ul>
     *   <li>Поддержка VST плагинов через {@link AudioHost}</li>
     *   <li>Точный контроль буфера ({@link #MEDIA_PLAYER_BLOCK_SIZE_FRAMES})</li>
     *   <li>Сkip intro/дроп/пит через waveform анализ</li>
     *   <li>Смена аудиовыходов ({@code setAudioOutput()})</li>
     * </ul>
     */
    public MediaPlayer mediaPlayer;
    /**
     * Глобальная карта параметров плеера.
     * <p>
     * <b>Хранимые параметры:</b>
     * </p>
     * <table>
     *   <tr><th>Ключ</th><th>Тип</th><th>Описание</th></tr>
     *   <tr><td>"volume"</td><td>double</td><td>Громкость (0.0-1.0)</td></tr>
     *   <tr><td>"tempo"</td><td>float</td><td>Темп (0.5-2.0)</td></tr>
     *   <tr><td>"pan"</td><td>float</td><td>Панорама (-1.0..1.0)</td></tr>
     *   <tr><td>"pause"</td><td>boolean</td><td>Состояние паузы</td></tr>
     *   <tr><td>"audio_out"</td><td>String</td><td>Имя аудиоустройства</td></tr>
     * </table>
     */
    public TypicalMapWrapper<String> globalMap = new TypicalMapWrapper<>();
    /**
     * Текущий загруженный медиа-ресурс.
     * <p>
     * <b>Форматы:</b> file URI, HTTP URL, локальный путь после предзагрузки.
     * </p>
     * <p><b>Использование:</b> передается в {@code new MediaPlayer(hit, blockSize)}</p>
     */
    public rf.ebanina.ebanina.Player.Media hit;
    /**
     * Список кастомных аудио-декодеров для нестандартных форматов.
     * <p>
     * <b>Поддержка:</b> MP3, WAV.
     * </p>
     * <p>
     * <b>Алгоритм выбора:</b> MediaPlayer перебирает {@code decoderList} по
     * {@link AudioDecoder#getFormat()} до первого совпадения с расширением файла.
     * </p>
     * <p><b>Расширение:</b> добавление новых декодеров без изменения MediaPlayer.</p>
     */
    public final List<AudioDecoder> decoderList = new ArrayList<>();
    /**
     * Автоматическая инициализация всех параметров MediaPlayer.
     * <p>
     * <b>Этапы настройки:</b>
     * </p>
     * <ol>
     *   <li>Активация всех VST плагинов {@code AudioHost.vstPlugins}</li>
     *   <li>Применение глобальных параметров из {@code globalMap}</li>
     *   <li>Установка аудиовыхода (если настроен)</li>
     * </ol>
     * <p><b>Цепочка fluent API:</b> plugins → volume → tempo → pan → decoders.</p>
     * <p>Вызывается из {@link #mediaPlayerPrepare(MediaPlayer)} ()} при создании плеера.</p>
     */
    public void initializeAudioParameters(MediaPlayer mediaPlayer) {
        for(PluginWrapper pluginWrapper : AudioHost.instance.vstPlugins) {
            pluginWrapper.turnOn();
        }

        mediaPlayer
                // Плагины (Steinberg, Au, Ivl2 и типо того)
                .setPlugins(AudioHost.instance.vstPlugins)
                // Громкость
                .setVolume(globalMap.get("volume", double.class))
                // Темп
                .setTempo((globalMap.get("tempo", float.class)))
                // Панорамирование
                .setPan(globalMap.get("pan", float.class))
                // Декодеры
                .setDecoders(decoderList);

        // Аудио выход, указывается как название
        if(String.valueOf(globalMap.get("audio_out", String.class)) != null
                && !String.valueOf(globalMap.get("audio_out", String.class)).equals("")) {
            try {
                mediaPlayer.setAudioOutput(String.valueOf(globalMap.get("audio_out", String.class)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    // Дефолтный переключатель.
    // Просто вызвав его, плеер будет автоматически переключать трек на текущий указатель trackIter в PlayProcessor.
    // Запускает новую одинокую-задачу (LonelyThreadPool).
    // В сетевом проигрывании бывает дурка, когда плеер не останавливает предыдущий MediaPlayer поток.
    // Это необъяснимо, ибо при переключении хуй знает сколько раз вызывается mediaPlayer.stop();, что по логике останавливает плеер и его потоки, независимо от сторонних потоков.
    // Единственное, что может быть - поток "Одинокий", он закрывается и не успевает закрыть предыдущие потоки - но это хуйня, ибо потоки плеера закрываются при запуске нового одинокого-потока.
    // -------------------------------------------------
    /**
     * Дефолтный переключатель треков через {@code trackIter}.
     * <p>
     * <b>Критическая thread-safety:</b> выполняется в {@link LonelyThreadPool}
     * (один трек за раз). Решает race conditions сетевого воспроизведения.
     * </p>
     * <p>
     * <b>Коррекция границ:</b>
     * </p>
     * <ul>
     *   <li><code>trackIter ≥ size</code>: loop→0 / next()</li>
     *   <li><code>trackIter < 0</code>: loop→size-1 / down()</li>
     * </ul>
     * <p><b>Цепочка:</b> коррекция → {@link PlaylistController#checkIndexOutOfBoundPlaylist()}} → {@link #_track(Track)}.</p>
     */
    public void _track() {
        _trackSingleAloneThread.runNewTask(() -> {
            // Проверка на выход за границы
            if(playProcessor.getTrackIter() >= playProcessor.getTracks().size()) {
                if (mediaParameters.get(MediaParameters.IS_PLAYLIST_LOOP.code, BooleanProperty.class).getValue().equals(true)) {
                    playProcessor.setTrackIter(0);
                } else {
                    PlaylistController.playlistController.next();
                }
            } else if(playProcessor.getTrackIter() < 0) {
                if(getPlaylistLoopProperty().getValue().equals(true)) {
                    playProcessor.setTrackIter(PlayProcessor.playProcessor.getTracks().size() - 1);
                } else {
                    PlaylistController.playlistController.down();
                }
            }

            // Проверка на выход за пределы плейлиста
            checkIndexOutOfBoundPlaylist();

            _track(playProcessor.getTracks().get(playProcessor.getTrackIter()));
        }, () -> {
//            mediaPlayer.stop();
//            mediaPlayer.close();
//            mediaPlayer.dispose();
        });
    }
    /**
     * Подготовка трека с задержкой (2s) и полным UI синхронизацией.
     * <p>
     * <b>TODO (критично):</b> <u>СЛИТЬ СЕТЕВОЕ+ЛОКАЛЬНОЕ</u> — дублирование кода!
     * Универсальность через {@code if(track.isNetty())}.
     * </p>
     * <p>
     * <b>Полный пайплайн (последовательно):</b>
     * </p>
     * <ol>
     *   <li><code>Thread.sleep(delay_between_play=2000ms)</code></li>
     *   <li>ListView focus: {@code similar|tracksListView.select(trackIter)}</li>
     *   <li>Аудио: {@link #prepareToPlay(Track)}}</li>
     *   <li>UI: {@link #updateInfo(Track)}} (обложка+метаданные)</li>
     * </ol>
     * <p><b>Вызывается:</b> {@link #_track()} → коррекция индекса → <b>этот метод</b>.</p>
     */
    public void _track(Track track) {
        // Задержка
        int delay = ConfigurationManager.instance.getIntItem("delay_between_play", "2000");

        if(delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Включение
        if (mediaPlayer != null) {

            // Фокус ячейки
            // TODO: Перевести в свойство, дабы не мусорить поток FX
            Platform.runLater(() -> {
                if(track.isNetty()) {
                    rootImpl.similar.getTrackListView().getSelectionModel().select(playProcessor.getTrackIter());
                } else {
                    rootImpl.tracksListView.getTrackListView().getSelectionModel().select(playProcessor.getTrackIter());
                }
            });

            // Сам плеер
            prepareToPlay(track);

            // Графическая хуйня (обложка, название, автор и т.д.)
            updateInfo(track);
        }
    }
    /**
     * Полная синхронизация UI + очистка кэша обложек.
     * <p>
     * <b>Двойственная логика:</b> {@code track.isNetty() ? сетевой : локальный}
     * </p>
     * <table>
     *   <tr><th>Сетевой</th><th>Локальный</th></tr>
     *   <tr><td><code>netty_file_path</code></td><td><code>track.getPath()</code></td></tr>
     *   <tr><td><code>mediaPlayer.getOverDuration()</code></td><td><code>track.getFormattedTotalDuration()</code></td></tr>
     * </table>
     * <p>
     * <b>Кэш обложек:</b> {@code trackCacheIter > Track.CACHE_SIZE} →
     * <code>albumArt=null, mipmap=null</code> для всех треков.
     * </p>
     * <p><b>Опции:</b> перевод заголовка, {@code PreviewPopupService.updateAll()}.</p>
     */
    public void updateInfo(Track track) {
        // Обновить обложку и цветовую гамму плеера
        rootImpl.artProcessor.initArt(track);

        // Ветвление на сетевой и локальный трек
        if(track.isNetty()) {
            Platform.runLater(() -> {
                rootImpl.currentArtist.setText(track.getArtist());
                rootImpl.currentTrackName.setText(track.getTitle().replace("-", ""));

                rootImpl.soundSlider.setMax(mediaPlayer.getOverDuration().toSeconds());
                rootImpl.endTime.setText(rf.ebanina.ebanina.Player.Track.getFormattedTotalDuration((int) rootImpl.soundSlider.getMax()));

                if(track.metadata.get("netty_file_path", String.class).equalsIgnoreCase("null")) {
                    rootImpl.soundSlider.setupDefaultBox();
                } else {
                    rootImpl.loadRectangleOfGainVolumeSlider(new File(track.metadata.get("netty_file_path", String.class)));
                }
            });
        } else {
            rootImpl.loadRectangleOfGainVolumeSlider(new File(track.getPath()));

            String endTime = track.getFormattedTotalDuration();
            String author = new String(track.getArtist().getBytes(), StandardCharsets.UTF_8);
            String title = new String(track.getTitle().getBytes(), StandardCharsets.UTF_8);
            String startTime = Track.getFormattedTotalDuration((int) mediaPlayer.getCurrentTime().toSeconds());

            playProcessor.getTracks().get(playProcessor.getTrackIter()).setArtist(author);
            playProcessor.getTracks().get(playProcessor.getTrackIter()).setTitle(title);

            Platform.runLater(() -> {
                rootImpl.endTime.setText(endTime);
                rootImpl.beginTime.setText(startTime);
                rootImpl.currentArtist.setText(author);
                rootImpl.currentTrackName.setText(title);
            });
        }

        PreviewPopupService.updateAll();

        if(trackCacheIter.get() > Track.CACHE_SIZE) {
            for (Track track_ : playProcessor.getTracks()) {
                track_.setAlbumArt(null);
                track_.setMipmap(null);
            }

            trackCacheIter.set(0);
        }

        trackCacheIter.incrementAndGet();

        if(ConfigurationManager.instance.getBooleanItem("translate_track_title", "false")) {
            rootImpl.currentTrackName.setText(Translator.instance.TranslateNodeText(
                    playProcessor.getTracks().get(playProcessor.getTrackIter()).getTitle(),
                    LocalizationManager.instance.lang.substring(LocalizationManager.instance.lang.indexOf("_") + 1)
            ));
        }
    }
    /**
     * Асинхронный парсинг URL через first-win стратегию.
     * <p>
     * <b>Алгоритм:</b> {@code invokeAny(playersMap)} — первый успешный сервис
     * (VK, Spotify, SoundCloud, etc) возвращает {@code Track} с прямой ссылкой.
     * </p>
     * <p>
     * <b>Параллельность:</b> все {@code IInfo.getTrackDownloadLink()} одновременно →
     * первый не-null URL выигрывает.
     * </p>
     * <p><b>Используется:</b> {@link #regenerateMediaPlayer(Track)}} для сетевых треков
     * без прямого пути.</p>
     * @throws IOException все сервисы вернули null
     */
    public Track trackParseAsync(String track) throws IOException {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            try {
                List<Callable<Track>> tasks = new ArrayList<>();

                for (Info.IInfo a : playersMap.values()) {
                    tasks.add(() -> {
                        Track url = a.getTrackDownloadLink(track);

                        if (url == null) {
                            throw new IOException("Invalid URL");
                        }

                        return url;
                    });
                }

                return executor.invokeAny(tasks);
            } catch (InterruptedException | ExecutionException e) {
                Music.mainLogger.warn("Не удалось получить URL для трека: " + track);
                return null;
            } finally {
                executor.shutdownNow();
            }
        }
    }
    /**
     * Универсальный resolver URL для треков (локальный/сетевой).
     * <p>
     * <b>Логика fallback:</b>
     * </p>
     * <ol>
     *   <li><code>track.toString()</code> — прямой URL</li>
     *   <li><b>Если null/URI_NULL:</b> {@link #trackParseAsync(String)}} по всем сервисам</li>
     *   <li>Лог ошибок + <code>null</code> при полном провале</li>
     * </ol>
     * <p><b>Используется:</b> {@link #regenerateMediaPlayer(Track)}} для сетевых треков.</p>
     */
    public URL getURIFromTrack(rf.ebanina.ebanina.Player.Track newValue) {
        try {
            String urlString = newValue.toString();

            if (urlString == null || urlString.equalsIgnoreCase(Info.PlayersTypes.URI_NULL.getCode())) {

                urlString = Objects.requireNonNull(trackParseAsync(newValue.viewName())).getPath();

                if (urlString == null) {
                    Music.mainLogger.println(Prefix.ERROR, "URL для загрузки отсутствует");

                    return null;
                }
            }

            return new URL(urlString);
        } catch (IOException e) {
            Music.mainLogger.err(e);

            return null;
        }
    }
    /**
     * Режим предзагрузки сетевых треков в локальный кэш.
     * <p>
     * <b>true:</b> URL → [64-файл кэш] → <code>local MP3</code> → 100% метаданные
     * <p>
     * <b>false:</b> URL → stream → <code>totalDuraSec</code> из Track парсинга
     * </p>
     * <p><b>Автоочистка:</b> {@code FileManager.isOccupiedSpace(64)} → clearCache().</p>
     */
    final boolean isPreDownload = ConfigurationManager.instance.getBooleanItem("network_pre_download", "false");
    /**
     * Полная регенерация MediaPlayer для нового трека.
     * <p>
     * <b>TODO (высокий приоритет):</b> слить сетевое+локальное через {@code Track.isNetty()}
     * </p>
     * <p>
     * <b>Критический пайплайн:</b>
     * </p>
     * <ol>
     *   <li>Сохранение состояния паузы в {@code globalMap}</li>
     *   <li><b>Сетевой:</b> {@link #getURIFromTrack(Track)}} → [preDownload|stream] → media URI</li>
     *   <li><b>Локальный:</b> {@code File.toURI().toString()}</li>
     *   <li><code>stop()→dispose()→pause()</code> (тройная очистка потоков)</li>
     *   <li>{@link #setNewMedia(Media)}} + {@code totalDuraSec} (stream mode)</li>
     * </ol>
     * <p><b>PreDownload алгоритм (64 файла):</b></p>
     * <pre>
     * isOccupiedSpace(64) → clearCache() →
     * Path(track.viewName()+".mp3") →
     * Files.copy(res.openStream()) →
     * track.metadata.netty_file_path=localPath
     * </pre>
     */
    public void regenerateMediaPlayer(Track track) {
        // Запихать состояние плеера (пауза/плей)
        globalMap.put("pause",
                (mediaPlayer.getStatus() == rf.ebanina.ebanina.Player.MediaPlayer.Status.PAUSED
                        || mediaPlayer.getStatus() == rf.ebanina.ebanina.Player.MediaPlayer.Status.READY),
                boolean.class);

        // Ссылка
        String media = null;

        // Сетевой
        if(track.isNetty()) {
            try {
                // Если у трека нет пути, то получить через сетевые сервисы
                URL res = track.getPath() == null || track.getPath().equals(Info.PlayersTypes.URI_NULL.getCode())
                        ? getURIFromTrack(track) : new URL(track.getPath());

                // Может не получить ссылку
                if (res != null) {

                    // Скачивать перед проигрыванием.
                    // Бля, оно нормально будет работать только при true, ибо даже с JavaFX MediaPlayer без скачивания работал криво.
                    // Много нюансов, которых заёб полный учитывать (не всегда получается длительность, разные форматы потоков, разные типы форматов, разные заголовки, и прочая хуйня)
                    if (isPreDownload) {

                        // Очистить папку с треками, если больше чем 64 чего то
                        if(FileManager.instance.isOccupiedSpace(Resources.Properties.DEFAULT_INET_CACHE_PATH.getKey(), 64)) {

                            // Чистка
                            FileManager.instance.clearCacheData(Resources.Properties.DEFAULT_INET_CACHE_PATH.getKey());
                        }

                        // Подготовить место для файла трека
                        media = java.nio.file.Path.of(Resources.Properties.DEFAULT_INET_CACHE_PATH.getKey(), track.viewName() + ".mp3").toFile().getAbsolutePath();

                        // Удобно
                        final Path path = Paths.get(media);

                        // Если файл уже скачан, нехуй его скачивать ещё раз.
                        if(!Files.exists(path)) {

                            // Скачивание с сетевого потока на локальный путь
                            Files.copy(res.openStream(), path, StandardCopyOption.REPLACE_EXISTING);
                        }

                        // Путь к локальному файлу
                        track.metadata.put("netty_file_path", media, String.class);
                    } else {
                        // Может, и получится слушать как на стриминговых сервисах, но вряд-ли.
                        // Господь пастырь мой...
                        media = String.valueOf(res);

                        // Нужно для генерации стандартной SoundSlider.
                        // В действительности, нужен весь поток для обработки, поэтому приходится скачивать.
                        track.metadata.put("netty_file_path", "null", String.class);
                    }
                } else {
                    Music.mainLogger.warn("Failed to load");

                    // Такого трека не найдено

                    media = null;
                }

                // Остановка и пауза на новом треке.
                // Это конечно пиздец, но поток может не остановится.
                // Поток также проскипает байты, если не поставить на паузу
                MediaProcessor.mediaProcessor.mediaPlayer.stop();
                MediaProcessor.mediaProcessor.mediaPlayer.dispose();
                MediaProcessor.mediaProcessor.mediaPlayer.pause();

                // Генерация
                setNewMedia(hit = new Media(media, track.isNetty()));

                // Длительность из потока не вычислить.
                // Берётся длительность из самого трека, которая должна быть заложена при парсинге.
                // Может быть прокатит, и получится проиграть на не preDownload
                if(!isPreDownload) {
                    MediaProcessor.mediaProcessor.mediaPlayer.setTotalOverDuration(Duration.seconds(track.getTotalDuraSec()));
                    totalDuraSec.set(track.getTotalDuraSec());
                }
            } catch (NullPointerException | IOException e) {
                e.printStackTrace();
            }
        } else {

            // Не сетевой
            mediaPlayer.stop();
            mediaPlayer.dispose();

            media = new File(track.getPath()).toURI().toString();
        }

        if(media == null) {
            throw new RuntimeException("Media is null");
        }

        // Генерация
        setNewMedia(hit = new Media(media, track.isNetty()));
    }
    /**
     * JavaFX Property для длительности стриминговых треков.
     * <p>
     * <b>Назначение:</b> <u>Только для {@code !isPreDownload}</u> — метаданные недоступны из потока.
     * </p>
     * <p><b>Источник:</b> {@code track.getTotalDuraSec()} из парсинга (HitMos/Spotify API).</p>
     * <p><b>Использование:</b> {@code mediaPlayer.setTotalOverDuration(Duration.seconds(totalDuraSec.get()))}</p>
     * <p><b>UI binding:</b> SoundSlider, endTime от этого значения при стриме.</p>
     */
    public SimpleIntegerProperty totalDuraSec = new SimpleIntegerProperty(0);
    /**
     * Финальная подготовка трека к воспроизведению.
     * <p>
     * <b>Три этапа (строго последовательно):</b>
     * </p>
     * <ol>
     *   <li>{@link #regenerateMediaPlayer(Track)}} — новый MediaPlayer</li>
     *   <li>{@link #readState(Track)}} — восстановление кэша (tempo/volume/time)</li>
     *   <li><code>!globalMap.pause</code> → {@link #fadePlay(float, float, boolean)}}</li>
     * </ol>
     * <p><b>Вызывается:</b> {@link #_track(Track)} после задержки 2s.</p>
     */
    public void prepareToPlay(Track track) {
        // Генерация нового плеера
        regenerateMediaPlayer(track);

        // Кэш (время, темп, прочая хуйня)
        readState(track);

        // Если в текущее проигрывание - пауза, то не включать
        if (!globalMap.get("pause", boolean.class)) {

            // Анимация громкости вручную
            fadePlay(0, 1, true);
        }
    }
    /**
     * Ручная анимация громкости (60 FPS, 500ms).
     * <p>
     * <b>Параметры анимации:</b> 17 кадров (500ms/60fps), линейная интерполяция.
     * </p>
     * <p>
     * <b>Логика:</b>
     * </p>
     * <ol>
     *   <li><code>setLineVolume(start)</code></li>
     *   <li><code>intro+file?</code> → <code>playIntro()→play()</code></li>
     *   <li>Thread: <code>volume = start + t*(end-start)</code></li>
     * </ol>
     * <p><b>Вызывается:</b> {@link #prepareToPlay(Track)}} (0→1, intro), {@link #pause_play()} (resume).</p>
     */
    public void fadePlay(float startVolume, float endVolume, boolean intro) {
        final int durationMs = 500;
        final int framesPerSecond = 60;
        final int frameTimeMs = 1000 / framesPerSecond;
        final int totalFrames = durationMs / frameTimeMs;

        mediaPlayer.setLineVolume(startVolume);

        if(intro && mediaPlayer.getMedia().getIntroSoundFile().exists()) {
            mediaPlayer.playIntro();
            mediaPlayer.play();
        } else {
            mediaPlayer.play();
        }

        new Thread(() -> {
            for (int frame = 0; frame <= totalFrames; frame++) {
                float t = (float) frame / totalFrames;
                float volume = startVolume + t * (endVolume - startVolume);
                mediaPlayer.setVolume(volume);

                try {
                    Thread.sleep(frameTimeMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            mediaPlayer.setVolume(endVolume);
        }).start();
    }
    /**
     * Toggle play/pause с fade-эффектом.
     * <p>
     * <b>Состояния:</b>
     * </p>
     * <table>
     *   <tr><th>Status</th><th>Действие</th></tr>
     *   <tr><td><code>PAUSED|READY</code></td><td><code>fadePlay(0→1, no intro)</code></td></tr>
     *   <tr><td><code>PLAYING</code></td><td><code>pause()</code></td></tr>
     * </table>
     */
    @KeyBind(id = "mizuka_play_hotkey", keys = {NativeKeyEvent.VC_MEDIA_PLAY}, global = true)
    @KeyBind(id = "mizuka_play_hotkey1", keys = {NativeKeyEvent.VC_NUM_LOCK}, global = true)
    public void pause_play() {
        if(mediaPlayer.getStatus().equals(rf.ebanina.ebanina.Player.MediaPlayer.Status.PAUSED)
                || mediaPlayer.getStatus().equals((rf.ebanina.ebanina.Player.MediaPlayer.Status.READY))) {
            fadePlay(0, 1, false);
        } else {
            pause();
        }
    }
    /**
     * Обычная пауза без fade.
     * <p><b>Null-safe:</b> проверка {@code mediaPlayer != null}.</p>
     * <p><b>Используется:</b> экстренная остановка, перемотка.</p>
     */
    public void pause() {
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }
    /**
     * Изменение темпа с пересчетом длительности и визуальным эффектом.
     * <p>
     * <b>Цепная реакция:</b>
     * </p>
     * <ol>
     *   <li><code>mediaPlayer.setTempo(tempo)</code></li>
     *   <li><code>globalMap.tempo = tempo</code> (сохранение)</li>
     *   <li><code>recalculateOverDuration()</code> → новая длительность</li>
     *   <li>UI: <code>SoundSlider.max + endTime</code></li>
     *   <li><b>VizFX:</b> <code>ColorProcessor.core.scaleHue(tempo)</code></li>
     * </ol>
     * <p><b>Null-safe.</b></p>
     */
    public void setTempo(float tempo) {
        if (mediaPlayer != null) {
            mediaPlayer.setTempo(tempo);
            globalMap.put("tempo", tempo, float.class);

            double durationSeconds = mediaPlayer.recalculateOverDuration().toSeconds();

            Platform.runLater(() -> {
                rootImpl.soundSlider.setMax(durationSeconds);
                rootImpl.endTime.setText(Track.getFormattedTotalDuration((float) rootImpl.soundSlider.getMax()));
            });

            ColorProcessor.core.scaleHue(tempo);
        }
    }
    /**
     * Инициализация глобальных параметров из постоянного хранилища.
     * <p>
     * <b>Загружаемые параметры:</b>
     * </p>
     * <table>
     *   <tr><th>Ключ</th><th>Источник</th><th>Назначение</th></tr>
     *   <tr><td><code>volume</code></td><td>FileManager.readSharedData()</td><td>mediaPlayer.setVolume()</td></tr>
     *   <tr><td><code>pitch/pan/tempo</code></td><td>FileManager.readSharedData()</td><td>mediaPlayer.setPitch/Pan/Tempo()</td></tr>
     *   <tr><td><code>pause</code></td><td>FileManager.readSharedData()</td><td>globalMap.pause (восстановление)</td></tr>
     *   <tr><td><code>audio_out</code></td><td>ConfigurationManager</td><td>mediaPlayer.setAudioOutput()</td></tr>
     * </table>
     */
    public void initGlobalMap() {
        globalMap.put("volume", Double.parseDouble(FileManager.instance.readSharedData().get("volume")), double.class);
        globalMap.put("pitch", Float.parseFloat(FileManager.instance.readSharedData().get("pitch")), float.class);
        globalMap.put("pan", Float.parseFloat(FileManager.instance.readSharedData().get("pan")), float.class);
        globalMap.put("pause", Boolean.parseBoolean(FileManager.instance.readSharedData().get("pause")), boolean.class);
        globalMap.put("tempo", Float.parseFloat(FileManager.instance.readSharedData().get("tempo")), float.class);
        globalMap.put("audio_out", ConfigurationManager.instance.getItem("audio_out", ""), String.class);
    }
    /**
     * Восстановление плеера из последнего сеанса (startup).
     * <p>
     * <b>Последовательность инициализации:</b>
     * </p>
     * <ol>
     *   <li>Чтение <code>last_track_time</code> из постоянного хранилища</li>
     *   <li>{@link MediaProcessor#_track()} — загрузка трека</li>
     *   <li>Настройка <code>onReady</code>: pause/play по {@code globalMap.pause}</li>
     *   <li>UI синхронизация: SoundSlider + ListView focus + beginTime</li>
     * </ol>
     * <p><b>Назначение:</b> мгновенное восстановление позиции после перезапуска.</p>
     */
    public void updateMediaPlayer() {
        double sec = Double.parseDouble(FileManager.instance.readSharedData().get("last_track_time"));

        mediaProcessor._track();

        if (globalMap.get("pause", boolean.class)) {
            mediaPlayer.setOnReady(() -> mediaPlayer.pause());
        } else {
            mediaPlayer.setOnReady(() -> mediaPlayer.play());
        }

        Platform.runLater(() -> {
            rootImpl.soundSlider.setValue(sec);
            rootImpl.soundSlider.setMax(MetadataOfFile.iMetadataOfFiles.getDuration(playProcessor.getTracks().get(playProcessor.getTrackIter()).toString()));

            rootImpl.tracksListView.getTrackListView().getSelectionModel().select(playProcessor.getTrackIter());
            rootImpl.beginTime.setText((Track.getFormattedTotalDuration(sec)));
        });
    }
    /**
     * Восстановление состояния трека из кэша + интеллектуальный старт.
     * <p>
     * <b>Двухуровневая система кэша:</b>
     * </p>
     * <table>
     *   <tr><th>Сетевой</th><th>Локальный</th></tr>
     *   <tr><td><code>INET_TRACKS_CACHE_PATH</code></td><td><code>CACHE_TRACKS_PATH/playlistName</code></td></tr>
     *   <tr><td><code>track.viewName()</code></td><td><code>track.getPath()</code></td></tr>
     * </table>
     * <p><b>Автоочистка:</b> {@code history.size() ≥ 25} → clear().</p>
     * <p>
     * <b>Восстанавливаемые параметры:</b> tempo/volume/pan/pitch →
     * Consumer&lt;String&gt; маппинг → globalMap + mediaPlayer.set*().
     * </p>
     * <p><b>Режимы старта (start_play_from):</b></p>
     * <table>
     *   <tr><th>Режим</th><th>Действие</th></tr>
     *   <tr><td><code>last_time</code></td><td>Время из кэша (&lt;total-30s)</td></tr>
     *   <tr><td><code>skip_intro/pit/drop</code></td><td>Waveform скип → setCurrentTime()</td></tr>
     *   <tr><td><code>like_moment</code></td><td>"like_moment_start" из файла</td></tr>
     * </table>
     * <p><b>Автовоспроизведение:</b> IS_AUTO_PLAYBACK → start/stop время из кэша.</p>
     */
    public void readState(Track track) {
        if (PlayProcessor.playProcessor.getTrackHistoryGlobal().size() >= ConfigurationManager.instance.getIntItem("global_history_size", "25")) {
            PlayProcessor.playProcessor.getTrackHistoryGlobal().getHistory().clear();
        }

        Path path;

        String trackInFile = track.getPath();

        final String al = ConfigurationManager.instance.getItem("start_play_from", "last_time");

        // Путь к кеш-файлу
        if(playProcessor.isNetwork()) {
            path = Path.of(
                    Resources.Properties.DEFAULT_INET_TRACKS_CACHE_PATH.getKey()
            ).toAbsolutePath();

            trackInFile = track.getViewName();
        } else {
            path = Path.of(
                    Resources.Properties.DEFAULT_CACHE_TRACKS_PATH.getKey(),
                    FileManager.instance.name(playProcessor.getCurrentPlaylist().get(playProcessor.getCurrentPlaylistIter()).getName())
            ).toAbsolutePath();
        }

        Map<String, String> trackArray = new HashMap<>();

        if (track.isRestoreState()) {
            Map<String, String> state = FileManager.instance.readArray(
                    path.toString(),
                    trackInFile,
                    Map.of()
            );

            if (!state.isEmpty()) {
                // Заготовка действий, на случай если есть кеш в файле
                Map<String, Consumer<String>> stateItems = new HashMap<>(Map.ofEntries(
                        Map.entry(DataTypes.TEMPO.code, (val) -> {
                            globalMap.put("tempo", Float.parseFloat(val), float.class);
                            mediaPlayer.setTempo(Float.parseFloat(val));
                        }),
                        Map.entry(DataTypes.VOLUME.code, (val) -> {
                            globalMap.put("volume", Double.parseDouble(val), double.class);
                            mediaPlayer.setVolume(Double.parseDouble(val));
                        }),
                        Map.entry(DataTypes.PAN.code, (val) -> {
                            globalMap.put("pan", Float.parseFloat(val), float.class);
                            mediaPlayer.setPan(Float.parseFloat(val));
                        }),
                        Map.entry(DataTypes.PITCH.code, (val) -> {
                            globalMap.put("pitch", Float.parseFloat(val), float.class);
                            mediaPlayer.setPan(Float.parseFloat(val));
                        })
                ));

                // Чтение кеша
                trackArray = FileManager.instance.readArray(
                        path.toAbsolutePath().toString(),
                        trackInFile,
                        Map.of()
                );

                for (Map.Entry<String, Consumer<String>> item : stateItems.entrySet()) {
                    if (state.get(item.getKey()) != null) {
                        String val = trackArray.get(item.getKey());

                        if (val != null) {
                            item.getValue().accept(val);
                        }
                    }
                }
            }
        }

        switch (al) {
            case "last_time" -> {
                if (track.isRestoreState() && PlayProcessor.playProcessor.getTrackHistoryGlobal().contains(track)) {
                    Duration dura = Duration.seconds(Double.parseDouble(trackArray.getOrDefault(DataTypes.TIME.code, "0")));

                    if (dura.toSeconds() < mediaPlayer.getOverDuration().toSeconds() - 30 && dura.toSeconds() > -1) {
                        mediaPlayer.setStartTime(dura);
                    }
                }
            }
            case "skip_intro" -> rootImpl.soundSlider.setOnLoadedSliderBackground(() -> setCurrentTime(Duration.seconds(getSkipIntroPoint(track.getPath()))));
            case "skip_pit" -> rootImpl.soundSlider.setOnLoadedSliderBackground(() -> setCurrentTime(Duration.seconds(getSkipPitPoint(track.getPath(), mediaPlayer.getCurrentTime().toSeconds(), mediaPlayer.getOverDuration().toSeconds()))));
            case "skip_drop" -> rootImpl.soundSlider.setOnLoadedSliderBackground(() -> setCurrentTime(Duration.seconds(getSkipDropPoint(track.getPath(), mediaPlayer.getCurrentTime().toSeconds(), mediaPlayer.getOverDuration().toSeconds()))));
            case "like_moment" -> {
                Duration dura = Duration.seconds(Double.parseDouble(
                        FileManager.instance.read(path.toString(), playProcessor.getTracks().get(playProcessor.getTrackIter()).toString(), "like_moment_start", String.valueOf(rootImpl.soundSlider.getValue()))
                ));

                mediaPlayer.setStartTime(dura);
            }
        }

        if (getAutoPlaybackProperty().getValue()) {
            mediaPlayer.setStartTime(Duration.seconds(Double.parseDouble(
                    FileManager.instance.read(
                            path.toString(),
                            playProcessor.getTracks().get(playProcessor.getTrackIter()).getPath(),
                            fields.get(DataTypes.LIKE_MOMENT_START.code).getLocalName(),
                            "0"))
            ));

            mediaPlayer.setStopTime(Duration.seconds(Double.parseDouble(
                    FileManager.instance.read(
                            path.toString(),
                            playProcessor.getTracks().get(playProcessor.getTrackIter()).getPath(),
                            fields.get(DataTypes.LIKE_MOMENT_START.code).getLocalName(),
                            mediaPlayer.getOverDuration().toString()))
            ));
        }
    }
    /**
     * Перемотка с полной реинициализацией плеера.
     * <p>
     * <b>Полный reset пайплайн:</b>
     * </p>
     * <ol>
     *   <li><code>mediaPlayer.stop()→close()</code></li>
     *   <li><code>setNewMedia(hit)</code> — возврат к текущему медиа</li>
     *   <li>VST плагины: <code>setPlugins(AudioHost.vstPlugins)</code></li>
     *   <li><b>Stream fallback:</b> <code>!preDownload → totalDuraSec</code></li>
     *   <li><code>setStartTime(a)→play()</code></li>
     * </ol>
     * <p><b>Используется:</b> скипы интро/дроп/пит из {@code readState()}.</p>
     */
    public void setCurrentTime(Duration a) {
        mediaPlayer.stop();
        mediaPlayer.close();

        setNewMedia(hit);

        mediaPlayer.setPlugins(AudioHost.instance.vstPlugins);

        if(playProcessor.isNetwork() && !ConfigurationManager.instance.getBooleanItem("network_pre_download", "false")) {
            mediaPlayer.setTotalOverDuration(Duration.seconds(totalDuraSec.get()));
        }

        mediaPlayer.setStartTime(a);
        mediaPlayer.play();
    }

    /**
     * Создание нового MediaPlayer с автоматической настройкой.
     * <p>
     * <b>Параметры:</b> {@code hit} (текущий Media) + {@code MEDIA_PLAYER_BLOCK_SIZE_FRAMES}.
     * </p>
     * <p><b>Автоконфигурация:</b> {@link #onMediaPlayerCreate} → {@link #mediaPlayerPrepare(MediaPlayer)} ()}
     * (VST + globalMap + onPlaying/onEndOfMedia колбэки).</p>
     * <p><b>Гарантия:</b> всегда свежий плеер с актуальными настройками.</p>
     */
    public void setNewMedia(Media media) {
        onMediaPlayerCreate.accept(mediaPlayer = new MediaPlayer(hit = media, MEDIA_PLAYER_BLOCK_SIZE_FRAMES));
    }
    /**
     * Полная настройка слушателей MediaPlayer + привязка к UI.
     * <p>
     * <b>Этапы:</b>
     * </p>
     * <ol>
     *   <li>{@link #initializeAudioParameters(MediaPlayer)}} — VST+globalMap</li>
     *   <li>UI колбэки через <code>Platform.runLater()</code></li>
     * </ol>
     * <p>
     * <b>Ключевые слушатели:</b>
     * </p>
     * <ul>
     *   <li><code>onPlaying:</code> volume нормализация (0→1) + SoundSlider sync</li>
     *   <li><code>onStopTimeReached:</code> !IS_AUTO_PLAYBACK → prepareToPlay()</li>
     *   <li><code>onEndOfMedia:</code> random/loop + next() логика</li>
     * </ul>
     */
    public void mediaPlayerPrepare(MediaPlayer mediaPlayer) {
        initializeAudioParameters(mediaPlayer);

        double currentTimeSeconds = mediaPlayer.getCurrentTime().toSeconds();
        double durationSeconds = mediaPlayer.getOverDuration().toSeconds();

        Platform.runLater(() -> {
            mediaPlayer.setOnPlaying(() -> {
                if (globalMap.get("volume", double.class) <= 0 || globalMap.get("volume", double.class) >= 1.0) {
                    globalMap.put("volume", 1.0, double.class);
                }

                Platform.runLater(() -> {
                    rootImpl.soundSlider.setValue(currentTimeSeconds);
                    rootImpl.soundSlider.setMax(durationSeconds);
                });
            });

            mediaPlayer.setOnStopTimeReached(() -> {
                if (MediaProcessor.mediaProcessor.getAutoPlaybackProperty().getValue().equals(false)) {
                    prepareToPlay(PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()));
                }
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                if (getPlayRandomProperty().getValue().equals(false)) {
                    if (PlayProcessor.playProcessor.getTrackIter() >= PlayProcessor.playProcessor.getTracks().size()) {
                        if (getPlaylistLoopProperty().getValue().equals(true)) {
                            PlayProcessor.playProcessor.setTrackIter(0);
                        } else {
                            PlaylistController.playlistController.next();
                        }
                    }
                } else {
                    PlayProcessor.playProcessor.setTrackIter(new Random().nextInt(0, PlayProcessor.playProcessor.getTracks().size() - 1));
                }

                if (getAutoPlaybackProperty().getValue().equals(false)) {
                    PlayProcessor.playProcessor.next();
                }
            });
        });
    }
    /**
     * Callback автоматической настройки при создании MediaPlayer.
     * <p><b>Связь:</b> {@link #setNewMedia(Media)}} → <code>onMediaPlayerCreate.accept(newPlayer)</code>.</p>
     */
    public Consumer<MediaPlayer> onMediaPlayerCreate = this::mediaPlayerPrepare;
    /**
     * Флаг очистки кэша сэмплов после waveform анализа.
     * <p><b>Используется:</b> {@code getSubsets()} → <code>if(isClearSamples) soundSlider.clearSamples()</code>.</p>
     * <p><b>Конфиг:</b> <code>clear_samples=true</code> (по умолчанию).</p>
     */
    private final boolean isClearSamples;

    /**
     * Формирует упрощённое представление аудиосигнала для визуализации.
     * <p>
     * Метод считывает все сэмплы из указанного аудиофайла и агрегирует их
     * в укрупнённые временные подмножества (окна). Каждое значение выходного
     * массива соответствует среднему модулю амплитуды в одном таком окне, что
     * позволяет компактно отобразить форму волны трека (waveform) в компоненте
     * {@link SoundSlider}.
     * </p>
     *
     * <h3>Принцип работы</h3>
     * <ul>
     *   <li>Получает исходный массив сэмплов из {@link SoundSlider#getSamples(java.io.File)}.</li>
     *   <li>Определяет количество подмножеств на основе длины массива и частоты кадров
     *       медиаплеера ({@link MediaPlayer#getFrameRate()}) — примерно одно значение на кадр.</li>
     *   <li>Делит весь массив сэмплов на равные по длине сегменты и для каждого
     *       вычисляет среднее значение модуля амплитуды (среднее по |sample|).</li>
     *   <li>Находит максимальное значение среди средних амплитуд и нормализует
     *       весь массив так, чтобы этот максимум соответствовал уровню {@code 32768.0f}.</li>
     *   <li>Пересчитывает нормализованные значения в пиксели относительно текущего
     *       размера компонента {@link SoundSlider#getSize()} — используется половина высоты
     *       компонента как максимальная видимая амплитуда.</li>
     *   <li>При установленном флаге {@code isClearSamples} очищает исходные сэмплы
     *       через {@link SoundSlider#clearSamples()}.</li>
     * </ul>
     *
     * <h3>Назначение</h3>
     * <p>
     * Возвращаемый массив предназначен для графической отрисовки волновой формы
     * аудиодорожки (например, в виде последовательности вертикальных полос или линий),
     * синхронизированной с воспроизведением {@link MediaPlayer}.
     * </p>
     *
     * <h3>Особенности и ограничения</h3>
     * <ul>
     *   <li>Используется усреднение по модулю амплитуды, а не RMS, поэтому значения
     *       отражают субъективную «громкость» сегмента, но не энергию сигнала.</li>
     *   <li>Масштабирование привязано к текущей высоте компонента {@link SoundSlider};
     *       при изменении размера компонента массив рекомендуется пересчитать.</li>
     *   <li>Метод несколько раз обращается к {@link SoundSlider#getSamples(java.io.File)}
     *       внутри циклов; при необходимости оптимизации стоит кэшировать массив сэмплов
     *       в локальную переменную перед циклами.</li>
     * </ul>
     *
     * @param path        путь к аудиофайлу, для которого формируется массив амплитуд.
     * @param soundSlider компонент, предоставляющий доступ к сэмплам и размерам области отрисовки.
     * @param mediaPlayer медиаплеер, задающий частоту кадров для временного разбиения сигнала.
     * @return массив нормализованных и масштабированных средних амплитуд по подмножествам,
     *         готовый для использования при отрисовке волновой формы.
     * @since 0.1.4.x
     */
    public float[] getSubsets(String path, SoundSlider soundSlider, MediaPlayer mediaPlayer) {
        // 1. Загружаем файл по пути
        File file = new File(path);

        // 2. Считываем все сэмплы из файла и определяем:
        //    - сколько временных окон нужно (кол-во сэмплов / FPS плеера)
        //    - сколько сэмплов в каждом окне (общее кол-во / кол-во окон)
        int numSubsets = (int) (soundSlider.getSamples(file).length / mediaPlayer.getFrameRate());
        int subsetLength = soundSlider.getSamples(file).length / numSubsets;

        // 3. Создаём итоговый массив под все окна
        float[] subsets = new float[numSubsets];

        int s = 0;
        for (int i = 0; i < subsets.length; i++) {
            double sum = 0;

            // 4. Для каждого окна суммируем модули всех сэмплов в нём
            for (int k = 0; k < subsetLength; k++) {
                sum += Math.abs(soundSlider.getSamples(file)[s++]);
            }

            // 5. Среднее значение модуля = "громкость" этого окна
            subsets[i] = (float) (sum / subsetLength);
        }

        // 6. Ищем максимальную "громкость" среди всех окон (для нормализации)
        float normal = 0;
        for (float sample : subsets) {
            if (sample > normal)
                normal = sample;
        }

        // 7. Коэффициент нормализации: самый громкий = 32768 (макс. амплитуда PCM)
        normal = 32768.0f / normal;

        // 8. Нормализуем все окна + масштабируем под высоту слайдера
        //    (32768 = макс. амплитуда → height/2 пикселей)
        for (int i = 0; i < subsets.length; i++) {
            subsets[i] *= normal;
            subsets[i] = (subsets[i] / 32768.0f) * (soundSlider.getSize().height / 2);
        }

        // 9. Опционально: очищаем кэш сэмплов (чтобы не жрать память)
        if(isClearSamples) {
            soundSlider.clearSamples();
        }

        // 10. Возвращаем готовые высоты волны в пикселях для отрисовки/skip-intro
        return subsets;
    }
    /**
     * <h1>Intellectual Drop Detection</h1> — алгоритм поиска "дропа" через паттерн-распознавание.
     * <p>
     * <b>Музыкальный контекст:</b> дроп = момент перехода от тихой части (build-up)
     * к громкому основному биту. Алгоритм ищет паттерн: <b>тихо-тихо | ГРОМКО</b>.
     * </p>
     *
     * <h2>Входные данные</h2>
     * <p>На основе {@link #getSubsets(String, SoundSlider, MediaPlayer)} ()}: массив высот столбцов waveform (пиксели).</p>
     *
     * <h2>Алгоритм (паттерн 5 блоков)</h2>
     * <pre>
     * [i-2][i-1] [i]  [i+1][i+2]
     *  тихо   тихо    ?   ГРОМКО  ГРОМКО
     *   ↑similar     ↑bigDiff    ↑similar
     * </pre>
     *
     * <h3>Условия срабатывания:</h3>
     * <table>
     *   <tr><th>Группа</th><th>Разница</th><th>Диапазон</th></tr>
     *   <tr><td><code>prevSimilar</code></td><td><code>[i-2↔i-1], [i-1↔i]</code></td><td><b>1-3px</b> (тихо)</td></tr>
     *   <tr><td><code>nextSimilar</code></td><td><code>[i+1↔i+2], [i+2↔i+1]</code></td><td><b>1-3px</b> (тихо)</td></tr>
     *   <tr><td><code>bigDiff</code></td><td><code>[i-1↔i+1], [i-2↔i+2]</code></td><td><b>5-10px</b> (переход!)</td></tr>
     * </table>
     *
     * <h2>Псевдокод</h2>
     * <pre>{@code
     * currentIndex = (currentTime/duration) * n
     * for i = currentIndex to n-2:
     *     if prevSimilar(1-3px) && nextSimilar(1-3px) && bigDiff(5-10px):
     *         return (i/n) * duration  // секунды дропа
     * return 0 // дроп не найден
     * }</pre>
     *
     * <h2>Защита от edge cases</h2>
     * <ul>
     *   <li><code>currentIndex < 2 || > n-3</code>: недостаточно блоков → 0</li>
     *   <li>Начало/конец трека: игнорируется</li>
     * </ul>
     *
     * <h2>Точность и тюнинг</h2>
     * <table>
     *   <tr><th>Параметр</th><th>Значение</th><th>Назначение</th></tr>
     *   <tr><td>similarity</td><td><b>1-3px</b></td><td>стабильная амплитуда</td></tr>
     *   <tr><td>drop threshold</td><td><b>5-10px</b></td><td>резкий переход</td></tr>
     * </table>
     *
     * <p><b>Вызывается:</b> {@code readState("skip_drop")} → {@code setCurrentTime()}</p>
     *
     * @param path путь к аудио файлу
     * @param currentTime текущее время (с)
     * @param duration общая длительность (с)
     * @return секунды дропа или 0
     */
    public int getSkipDropPoint(String path, double currentTime, double duration) {
        final float[] subsets = getSubsets(path, rootImpl.soundSlider, mediaPlayer);

        int n = subsets.length;

        int currentIndex = (int) ((currentTime / duration) * n);
        // Чтобы брать 2 блока слева и справа, нужен запас индексов
        if (currentIndex < 2 || currentIndex > n - 3) {
            return 0; // Недостаточно данных для проверки паттерна
        }

        for (int i = currentIndex; i < n - 2; i++) {
            // Предыдущие два блока
            double prevDiff = Math.abs(subsets[i - 1] - subsets[i - 2]);
            double prevDiff2 = Math.abs(subsets[i] - subsets[i - 1]);

            // Следующие два блока
            double nextDiff = Math.abs(subsets[i + 1] - subsets[i + 2]);
            double nextDiff2 = Math.abs(subsets[i + 2] - subsets[i + 1]);

            // Похожесть предыдущих блоков: малая разница 1-3
            boolean prevSimilar = (prevDiff >= 1 && prevDiff <= 3) && (prevDiff2 >= 1 && prevDiff2 <= 3);
            // Похожесть следующих блоков: малая разница 1-3
            boolean nextSimilar = (nextDiff >= 1 && nextDiff <= 3) && (nextDiff2 >= 1 && nextDiff2 <= 3);

            // Разница между предыдущими и следующими группами: большая разница 5-10
            double betweenGroupDiff1 = Math.abs(subsets[i - 1] - subsets[i + 1]);
            double betweenGroupDiff2 = Math.abs(subsets[i - 2] - subsets[i + 2]);
            boolean bigDiff = (betweenGroupDiff1 >= 5 && betweenGroupDiff1 <= 10)
                    && (betweenGroupDiff2 >= 5 && betweenGroupDiff2 <= 10);

            if (prevSimilar && nextSimilar && bigDiff) {
                // Возвращаем позицию (во времени) обнаруженной ямы
                return (int) ((i / (double) n) * duration);
            }
        }

        return 0;
    }
    /**
     * Поиск первого значимого звука (конец интро).
     * <p>
     * <b>Музыкальный контекст:</b> интро = тихое вступление → первый вокал/бит.
     * </p>
     *
     * <h2>Алгоритм "Первый пик"</h2>
     * <p>Ищет первый столбец waveform с амплитудой ≥10px (полная высота = 20px).</p>
     *
     * <h3>Геометрия пикселей:</h3>
     * <pre>
     * height/2          ┌───┐
     *        posY ◄─────┤   │ next_sample
     *                   │   │
     *                   └───┘
     *                   ^
     *                negY    (negY-posY+2) ≥ 20px = ГРОМКИЙ ЗВУК!
     * </pre>
     *
     * <h2>Псевдокод</h2>
     * <pre>
     * for i = 0 to n-1:
     *     if waveform[i+1] ≥ (height/2 - 10px):
     *         return i  // индекс первого звука
     * return 0
     * </pre>
     *
     * <p><b>Вызывается:</b> {@code readState("skip_intro")} → {@code setCurrentTime()}.</p>
     */
    public int getSkipIntroPoint(String path) {
        final float[] subsets = getSubsets(path, rootImpl.soundSlider, mediaPlayer);

        for (int i = 0; i < subsets.length - 1; i++) {
            int next_sample = (int) subsets[i + 1];

            int posY = (rootImpl.soundSlider.getSize().height / 2) - next_sample;
            int negY = (rootImpl.soundSlider.getSize().height / 2) + next_sample;

            if((negY - posY + 2) >= 20) {
                return i;
            }
        }

        return 0;
    }
    /**
     * Выход из "пита" (тишины) через статистический анализ.
     * <p>
     * <b>Музыкальный контекст:</b> пит = внезапная тишина в припеве/дропе.
     * </p>
     *
     * <h2>Статистический алгоритм</h2>
     * <ol>
     *   <li>От текущего времени: вычислить diffs всех соседних столбцов</li>
     *   <li>Средняя разница: <code>avgDiff</code></li>
     *   <li>Первый столбец с <code>diff > avgDiff×2 + 1</code> = конец пита</li>
     * </ol>
     *
     * <h3>Псевдокод</h3>
     * <pre>
     * currentIndex = (currentTime/duration) * n
     * diffs = |subsets[i+1] - subsets[i]| от currentIndex
     * avgDiff = mean(diffs)
     *
     * for i = 0 to diffs.size():
     *     if diffs[i] > avgDiff*2 + 1:
     *         return (currentIndex+i+1)/n * duration
     * return 0
     * </pre>
     *
     * <h2>Защита edge cases</h2>
     * <ul>
     *   <li><code>currentIndex ≥ n-1</code>: конец трека → 0</li>
     * </ul>
     *
     * <p><b>Вызывается:</b> {@code readState("skip_pit")} → {@code setCurrentTime()}.</p>
     */
    public int getSkipPitPoint(String path, double currentTime, double duration) {
        final float[] subsets = getSubsets(path, rootImpl.soundSlider, mediaPlayer);

        int n = subsets.length;

        int currentIndex = (int) ((currentTime / duration) * n);
        if (currentIndex >= n - 1) {
            return 0;
        }

        List<Double> diffs = new ArrayList<>();
        for (int i = currentIndex; i < n - 1; i++) {
            double diff = Math.abs(subsets[i + 1] - subsets[i]);
            diffs.add(diff);
        }

        double avgDiff = diffs.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        int pitEndIndex = -1;
        for (int i = 0; i < diffs.size(); i++) {
            if (diffs.get(i) > (avgDiff * 2) + 1) {
                pitEndIndex = currentIndex + i + 1;
                break;
            }
        }

        if (pitEndIndex != -1) {
            double newTime = (pitEndIndex / (double) n) * duration;

            return (int) newTime;
        }

        return 0;
    }
    /**
     * Перегруженная версия {@link #getSkipIntroPoint(String)} с явной высотой.
     * <p>
     * <b>Гибкость:</b> позволяет использовать кастомную высоту SoundSlider
     * (при изменении размера UI).
     * </p>
     * <p><b>Геометрия:</b> <code>(height/2 ± next_sample) ≥ 20px</code> = первый звук.</p>
     * <p><b>Используется:</b> {@link #skipIntro(String)}} с реальной высотой слайдера.</p>
     */
    public int getSkipIntroPoint(String path, int height) {
        final float[] subsets = getSubsets(path, rootImpl.soundSlider, mediaPlayer);

        for (int i = 0; i < subsets.length - 1; i++) {
            int next_sample = (int) subsets[i + 1];

            int posY = (height / 2) - next_sample;
            int negY = (height / 2) + next_sample;

            if((negY - posY + 2) >= 20) {
                return i;
            }
        }

        return 0;
    }
    /**
     * Асинхронный скип интро с защитой от конца трека.
     * <p>
     * <b>Двухэтапная логика:</b>
     * </p>
     * <ol>
     *   <li><code>currentTime > total-10s</code> → <code>playProcessor.next()</code></li>
     *   <li>{@code skipExec.submit()} → {@link #setCurrentTime(Duration)}}</li>
     * </ol>
     * <p><b>Защита:</b> не скипает за 10s до конца (избегает повтора).</p>
     */
    public void skipIntro(String path) {
        if(mediaPlayer.getCurrentTime().toSeconds() > mediaPlayer.getOverDuration().toSeconds() - 10) {
            playProcessor.next();
        }

        skipExec.submit(() -> setCurrentTime(Duration.seconds(getSkipIntroPoint(path, rootImpl.soundSlider.getSize().height))));
    }
    /**
     * Асинхронный скип пита без дополнительных проверок.
     * <p>
     * <b>Простая логика:</b> {@code skipExec} → {@link #getSkipPitPoint(String, double, double)}} → {@link #setCurrentTime(Duration)}}.</p>
     * <p><b>Thread-safe:</b> не блокирует UI/MediaPlayer.</p>
     */
    public void skipPit(String path) {
        skipExec.submit(() -> {
            setCurrentTime(Duration.seconds(getSkipPitPoint(path, mediaPlayer.getCurrentTime().toSeconds(), mediaPlayer.getOverDuration().toSeconds())));
        });
    }
    /**
     * Асинхронный скип аутро через поиск тишины в конце.
     * <p>
     * <b>Музыкальный контекст:</b> аутро = затухание в конце трека.
     * </p>
     *
     * <h2>Алгоритм "Последние 20s → тишина"</h2>
     * <p>Проверяет последние 20 столбцов waveform:</p>
     * <pre>
     * if currentTime > (stopTime - 20s):
     *     for i = n-20 to n:
     *         if waveform[i] ≤ 3px (тишина):
     *             next() + break
     * </pre>
     *
     * <h3>Геометрия тишины:</h3>
     * <pre>
     * height/2          ┌───┐ ← sample ≤ 3px = АУТРО!
     *        posY ◄─────┤   │
     *                   │   │
     *                   └───┘
     *                   ^
     *                negY    (negY-posY+2) ≤ 6px = ТИШИНА
     * </pre>
     *
     * <p><b>Thread-safe:</b> {@code skipExec.submit()}.</p>
     */
    public void skipOutro(String path) {
        skipExec.submit(() -> {
            final float[] subsets = getSubsets(path, rootImpl.soundSlider, mediaPlayer);

            if(mediaPlayer.getCurrentTime().toSeconds() > (mediaPlayer.getStopTime().toSeconds() - 20)) {
                for (int i = subsets.length - 20; i < subsets.length; i++) {
                    int sample = (int) subsets[i];

                    int posY = (rootImpl.soundSlider.getSize().height / 2) - sample;
                    int negY = (rootImpl.soundSlider.getSize().height / 2) + sample;

                    if ((negY - posY + 2) <= 6) {
                        playProcessor.next();
                        break;
                    }
                }
            }
        });
    }

    /**
     * Инициализация UI обработчиков истории треков.
     * <p>
     * <b>Null-safe привязка:</b> <code>tracksHistory.setOnAction()</code> → ContextMenu.</p>
     * <p><b>Логика:</b> клик по истории → {@code TrackHistoryContextMenu.show()}.</p>
     * <p><b>Вызывается:</b> при старте приложения (post UI init).</p>
     */
    public void initialize() {
        if(rootImpl.tracksHistory != null) {
            rootImpl.tracksHistory.setOnAction((e) -> {
                if(playProcessor.getTrackHistoryGlobal() != null) {
                    playProcessor.getTrackHistoryGlobal().getTrackHistoryContextMenu().show(rootImpl.stage.getScene().getWindow());
                }
            });
        }
    }
}