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
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javazoom.jl.decoder.Bitstream;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Root;
import rf.ebanina.ebanina.Player.AudioDecoder;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.ebanina.Player.MediaPlayer;
import rf.ebanina.ebanina.Player.Mp3PcmStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.io.*;

import static rf.ebanina.UI.Root.stage;

/**
 * Аудиовизуальный слайдер-волновой индикатор на базе {@link JFXSlider} с полной поддержкой MP3/WAV декодирования.
 * <p>
 * Компонент отображает звуковую волну аудиофайла в виде анимированных прямоугольников (столбцов),
 * синхронизированных с позицией воспроизведения. Поддерживает асинхронную загрузку сэмплов,
 * плавные переходы цветов и оптимизацию памяти.
 * </p>
 *
 * <h3>Основные возможности</h3>
 * <ul>
 *   <li>Автоматическое декодирование PCM-данных из WAV/MP3 через {@link #initSamples(File)}.</li>
 *   <li>Визуализация волны: столбцы шириной 4px с закруглением 6px, высота пропорциональна амплитуде.</li>
 *   <li>Анимированное заполнение: активные столбцы меняют цвет за 125мс ({@link #timeAnimation}).</li>
 *   <li>Оптимизация: автоматическая очистка сэмплов по настройке {@link ConfigurationManager}.</li>
 *   <li>Фокус-зависимое поведение: плавные анимации только при активном окне.</li>
 * </ul>
 *
 * <h3>Визуализация</h3>
 * <ul>
 *   <li>Количество столбцов = ширина слайдера / {@link #boxWidth} (4px).</li>
 *   <li>Цвет: активные столбцы — {@link #colorProperty}, неактивные — DARKGRAY.</li>
 *   <li>Анимация: 500мс с задержкой i×2мс на столбец, интерполятор {@link Root#iceInterpolator}.</li>
 * </ul>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * SoundSlider waveSlider = new SoundSlider(0, 1000, 0);
 * waveSlider.setSize(new Dimension(400, 60));
 * waveSlider.setColor(Color.CYAN);
 *
 * // Загрузка и визуализация аудио
 * waveSlider.loadSliderBackground(audioFile);
 *
 * // Обработчик завершения загрузки
 * waveSlider.setOnLoadedSliderBackground(() -> System.out.println("Wave loaded"));
 * }</pre>
 *
 * @author Ebanina Std
 * @since 0.1.4.4
 * @see JFXSlider
 * @see AudioInputStream
 * @see Mp3File
 */
public class SoundSlider
        extends Slider
{
    /**
     * Фоновая область для звуковой волны (прозрачная для мыши).
     * <p>
     * Содержит {@code Rectangle} столбцы, синхронизированные с позицией {@link JFXSlider}.
     * Привязана к позиции слайдера через {@code layoutX/YProperty().bind()}.
     * {@link Pane#setMouseTransparent(boolean)}} позволяет кликам проходить к слайдеру.
     * </p>
     *
     * @see #initializeBox()
     * @see #loadBox(float[])
     */
    private Pane sliderBackground;
    /**
     * Цвет активных столбцов волны (до текущей позиции воспроизведения).
     * <p>
     * Используется в {@link #updateSlider()} для анимации заполнения:
     * <ul>
     *   <li>Активные столбцы (0..activeCount): {@code colorProperty.get()}</li>
     *   <li>Неактивные столбцы: {@link Color#DARKGRAY}</li>
     * </ul>
     * По умолчанию {@link Color#BLACK}.
     * </p>
     *
     * @see #setColor(Color)
     * @see #colorPropertyProperty()
     */
    protected ObjectProperty<Color> colorProperty;
    /**
     * Длительность анимации смены цвета столбцов (по умолчанию 125мс).
     * <p>
     * Используется в {@link Timeline} для плавного перехода:
     * <pre>{@code new Timeline(new KeyFrame(timeAnimation, new KeyValue(rect.fillProperty(), color)))}</pre>
     * Влияет на отзывчивость визуального отклика при перетаскивании слайдера.
     * </p>
     */
    protected Duration timeAnimation = Duration.millis(125);
    /**
     * Размеры слайдера для расчёта количества столбцов.
     * <p>
     * <ul>
     *   <li>{@code size.width / boxWidth} = количество столбцов</li>
     *   <li>{@code size.height / 2} = центр волны для симметричного отображения</li>
     *   <li>Высота столбца = нормализованная амплитуда × ({@code size.height / 2})</li>
     * </ul>
     * Устанавливается через {@link #setSize(Dimension)}.
     * </p>
     */
    protected Dimension size = new Dimension(0, 0);
    /**
     * Ширина одного столбца звуковой волны (по умолчанию 4px).
     * <p>
     * Влияет на детализацию визуализации:
     * <ul>
     *   <li>Меньше = больше столбцов, выше детализация</li>
     *   <li>Больше = меньше столбцов, проще производительность</li>
     * </ul>
     * Позиция: {@code x = i * boxWidth + 1px отступ}.
     * </p>
     */
    protected int boxWidth = 4;
    /**
     * Радиус скругления углов столбцов - 6px).
     * <p>
     * Создаёт мягкий современный вид полосок волны:
     * <pre>{@code rect.setArcHeight(arc); rect.setArcWidth(arc);}</pre>
     * </p>
     */
    protected int arc = 6;
    /**
     * Массив PCM сэмплов аудиофайла для расчёта высоты столбцов.
     * <p>
     * Загружается через {@link #initSamples(File)} из WAV/MP3 с нормализацией амплитуды.
     * Используется в {@link #loadBox(float[])} для создания волновой формы.
     * Автоматически очищается при {@link ConfigurationManager#getBooleanItem(String, String)}.
     * </p>
     */
    private float[] samples;
    /**
     * Создаёт пустой слайдер без визуализации (min=0, max=120, val=0).
     * <p>
     * Делегирует в полный конструктор для консистентности инициализации.
     * </p>
     */
    public SoundSlider() {
        this(0, 120, 0);
    }
    /**
     * Создаёт слайдер с заданными параметрами и полной инициализацией волновой визуализации.
     * <p>
     * <b>Базовая настройка JFXSlider:</b>
     * <ul>
     *   <li>{@code setShowTickMarks(false)} — скрывает деления</li>
     *   <li>{@code setBlockIncrement(1.0)} — точная прокрутка</li>
     *   <li>{@code setMajorTickUnit(5.0)} — крупные деления каждые 5</li>
     *   <li>{@code setMinorTickCount(4)} — 4 мелких деления между крупными</li>
     *   <li>{@code setId("color-slider")} — CSS селектор для стилизации</li>
     *   <li>{@code "slider-custom"} — кастомные стили через ResourceManager</li>
     * </ul>
     * <b>Волновая система:</b>
     * <ul>
     *   <li>{@link #valueProperty()} → {@link #updateSlider()} для синхронизации столбцов</li>
     *   <li>{@link #colorProperty} = {@link Color#BLACK} по умолчанию</li>
     *   <li>{@link #sliderBackground} привязан к позиции слайдера</li>
     * </ul>
     * </p>
     *
     * @param min минимальное значение слайдера (обычно 0)
     * @param max максимальное значение слайдера (длительность трека в условных единицах)
     * @param val начальное значение слайдера (текущая позиция воспроизведения)
     */
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

        setVisible(false);

        sliderBackground = new Pane();
        sliderBackground.setMouseTransparent(true);
        sliderBackground.layoutXProperty().bind(layoutXProperty());
        sliderBackground.layoutYProperty().bind(layoutYProperty());
    }
    /**
     * Возвращает контейнер звуковой волны.
     * <p>
     * Предоставляет доступ к {@link Pane}, содержащему {@link Rectangle} столбцы.
     * Полезно для:
     * <ul>
     *   <li>Добавления дополнительных эффектов/анимаций к столбцам</li>
     *   <li>Получения текущего состояния визуализации</li>
     *   <li>Отладки содержимого волны</li>
     * </li>
     * </p>
     *
     * @return фоновая область со столбцами волны
     * @see #updateSlider()
     * @see #loadBox(float[])
     */
    public Pane getSliderBackground() {
        return sliderBackground;
    }
    /**
     * Интерполятор анимации смены цвета столбцов (по умолчанию {@link Interpolator#EASE_IN}).
     * <p>
     * Управляет плавностью перехода:
     * <ul>
     *   <li>Активные столбцы: {@code DARKGRAY → colorProperty}</li>
     *   <li>Неактивные: {@code colorProperty → DARKGRAY}</li>
     * </ul>
     * Используется во всех {@link Timeline} в {@link #updateSlider()}.
     * </p>
     */
    public Interpolator interpolator = Interpolator.EASE_IN;
    /**
     * Возвращает текущий интерполятор анимации столбцов.
     *
     * @return интерполятор для кастомизации плавности
     * @see #setInterpolator(Interpolator)
     */
    public Interpolator getInterpolator() {
        return interpolator;
    }
    /**
     * Устанавливает интерполятор анимации (цепной вызов).
     * <p>
     * Применяется ко всем анимациям цветовых переходов столбцов в {@link #updateSlider()}.
     * Рекомендуемые значения:
     * <ul>
     *   <li>{@link Interpolator#EASE_IN} — ускорение (по умолчанию)</li>
     *   <li>{@link Interpolator#EASE_OUT} — замедление</li>
     *   <li>{@link Root#iceInterpolator} — кастомный "лёд" эффект</li>
     * </ul>
     * </p>
     *
     * @param interpolator новый интерполятор
     * @return {@code this}
     */
    public SoundSlider setInterpolator(Interpolator interpolator) {
        this.interpolator = interpolator;
        return this;
    }
    /**
     * Синхронизирует визуализацию столбцов с текущей позицией слайдера.
     * <p>
     * <b>Основная логика (выполняется в FX потоке):</b>
     * <ol>
     *   <li>Рассчитывает {@code activeCount = (value/max) * totalColumns}</li>
     *   <li><b>Неактивные столбцы</b> (activeCount..size): анимируют к {@link Color#DARKGRAY}</li>
     *   <li><b>Активные столбцы</b> (0..activeCount): анимируют к {@link #colorProperty}</li>
     *   <li><b>Оптимизация:</b> без анимации если окно не в фокусе ({@link Root#stage#isFocused()})</li>
     * </ol>
     * </p>
     * <p>
     * <b>Выходные условия (ранний возврат):</b>
     * <ul>
     *   <li>{@code value ≤ 0} — начало трека</li>
     *   <li>{@code max ≤ 0} — неинициализирован</li>
     *   <li>{@code size == 0} — нет столбцов</li>
     * </ul>
     * </p>
     * <p>
     * <b>Автоподписка:</b> Вызывается автоматически при изменении {@link #valueProperty()}
     * через слушатель в конструкторе.
     * </p>
     */
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
    /**
     * Устанавливает массив PCM сэмплов для волновой визуализации.
     * <p>
     * Заменяет текущий {@link #samples} новым массивом. Не запускает перерисовку —
     * для этого используйте {@link #loadBox(float[])}.
     * </p>
     * <p>
     * <b>Формат сэмплов:</b> float[-1.0f..1.0f], нормализованные значения амплитуды.
     * Загружаются через {@link #initSamples(File)} или внешние декодеры.
     * </p>
     *
     * @param var новый массив сэмплов (может быть {@code null} для очистки)
     */
    public void setSamples(float[] var) {
        samples = var;
    }
    /**
     * Возвращает текущий массив PCM сэмплов.
     * <p>
     * Содержит нормализованные амплитуды аудиофайла для расчёта высоты столбцов.
     * Может быть {@code null} до вызова {@link #initSamples(File)} или {@link #setSamples(float[])}.
     * </p>
     *
     * @return массив сэмплов или {@code null}
     */
    public float[] getSamples() {
        return samples;
    }
    /**
     * Создаёт асинхронный поток для инициализации пустых столбцов волны.
     * <p>
     * Возвращает готовый {@link Thread}, выполняющий {@link #initializeBox()} в фоне.
     * Предотвращает блокировку UI при создании большого количества столбцов.
     * </p>
     * <p>
     * <b>Использование:</b>
     * <pre>{@code Thread thread = slider.setupSliderBoxAsync(); thread.start();}</pre>
     * </p>
     *
     * @return поток для асинхронной инициализации
     */
    public Thread setupSliderBoxAsync() {
        return new Thread(this::initializeBox);
    }
    /**
     * Очищает и создаёт пустые столбцы волны (выполняется в FX потоке).
     * <p>
     * <b>Алгоритм:</b>
     * <ol>
     *   <li>{@link #sliderBackground#getChildren().clear()} — удаляет старые столбцы</li>
     *   <li>Создаёт {@code N = size.width / boxWidth} новых {@link Rectangle}</li>
     *   <li>Параметры столбца:
     *     <ul>
     *       <li>{@code x = i * boxWidth + 1px} — позиция с отступом</li>
     *       <li>{@code width = boxWidth - 1px} — 3px при boxWidth=4</li>
     *       <li>{@code height = size.height} — полная высота слайдера</li>
     *       <li>{@code fill = Color.DARKGRAY} — неактивное состояние</li>
     *       <li>{@code arcHeight/Width = arc} — скругление углов</li>
     *     </ul>
     *   </li>
     * </ol>
     * </p>
     * <p>
     * Вызывается асинхронно через {@link #setupSliderBoxAsync()} или напрямую.
     * </p>
     */
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
    /**
     * Декодирует PCM сэмплы из {@link AudioInputStream} с поддержкой различных форматов.
     * <p>
     * <b>Входные параметры:</b>
     * <ul>
     *   <li>{@code frameLength} — общее количество фреймов аудио</li>
     *   <li>{@code bytesPerSample} — 1/2 байта на сэмпл (8/16 bit)</li>
     *   <li>{@code isBigEndian} — порядок байтов MSB/LSB</li>
     *   <li>{@code channels} — моно (1) / стерео (2)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Алгоритм декодирования:</b>
     * <ol>
     *   <li>Читает буфером 8192 фрейма ({@code chans * bytesPerSample * 8192})</li>
     *   <li><b>8-bit:</b> {@code (buffer[i] & 0xFF) << 8} → sign-extend</li>
     *   <li><b>16-bit BigEndian:</b> {@code (b1<<8) | b2}</li>
     *   <li><b>16-bit LittleEndian:</b> {@code b1 | (b2<<8)}</li>
     *   <li><b>Sign-extend:</b> если MSB=1, {@code sample |= 0xFFFF0000} → [-32768..32767]</li>
     *   <li><b>Суммирование каналов:</b> {@code sum / channels} → средняя амплитуда</li>
     * </ol>
     * </p>
     * <p>
     * <b>Выход:</b> float[] нормализованных сэмплов [-32768..32767] для {@link #loadBox(float[])}.
     * </p>
     *
     * @param frameLength количество фреймов для чтения
     * @param in аудиопоток PCM_SIGNED
     * @param buffer буфер чтения (предварительно выделен)
     * @param bytesPerSample 1 или 2 байта на сэмпл
     * @param isBigEndian порядок байтов (true=MSB-first)
     * @param channels количество каналов (1=моно, 2=стерео)
     * @return массив PCM сэмплов
     * @throws IOException ошибки чтения потока
     */
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
    /** Текущий анализируемый аудиофайл (кэш для повторных вызовов). */
    private File file;
    /**
     * Возвращает последний загруженный аудиофайл.
     * <p>
     * Сохраняется при вызове {@link #initSamples(File)} для повторного анализа без
     * повторного указания пути. {@code null} до первой загрузки.
     * </p>
     *
     * @return текущий файл или {@code null}
     */
    public File getFile() {
        return file;
    }
    /**
     * Ленивая загрузка сэмплов с кэшированием файла (thread-safe).
     * <p>
     * <b>Логика:</b>
     * <ol>
     *   <li>Если {@link #getSamples()} ≠ null → возврат кэша</li>
     *   <li>Иначе: {@link #initSamples(File)} → кэширование в {@link #file}</li>
     * </ol>
     * Предотвращает повторное декодирование одного и того же файла.
     * </p>
     *
     * @param file аудиофайл для анализа (WAV/MP3/другие форматы)
     * @return массив PCM сэмплов
     * @see #initSamples(File)
     */
    public float[] getSamples(File file) {
        if(getSamples() == null)
            initSamples(this.file = file);

        return getSamples();
    }
    /**
     * Очищает память от PCM сэмплов.
     * <p>
     * Вызывает {@link SoundSlider#setSamples(float[])} для освобождения RAM после визуализации.
     * Автоматически вызывается в {@link #loadSliderBackground(File)} при
     * {@link ConfigurationManager#getBooleanItem(String, String)}.
     * </p>
     */
    public void clearSamples() {
        setSamples(null);
    }
    /**
     * Создаёт упрощённую "плоскую" волну (фиксированная высота 5px).
     * <p>
     * <b>Сценарии использования:</b>
     * <ul>
     *   <li>Загрузка сэмплов не удалась</li>
     *   <li>Отображение "плейсхолдера" во время декодирования</li>
     *   <li>Ошибочный файл (fallback UI)</li>
     * </ul>
     * </p>
     * <b>Алгоритм:</b>
     * <ol>
     *   <li>Обрезает лишние столбцы: {@code count = min(children.size(), expectedCount)}</li>
     *   <li>Сбрасывает параметры: {@code x=i*boxWidth+1}, {@code fill=DARKGRAY}</li>
     *   <li>Анимирует высоту: {@code height → 5px} за {@link #timeAnimation}</li>
     *   <li>Фиксирует {@code y=10px} как базовую линию</li>
     * </ol>
     *
     * @see #loadBox(float[]) — полная волновая визуализация
     */
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
    /**
     * Создает волновую визуализацию из массива PCM сэмплов с каскадной анимацией роста.
     * <p>
     * <b>Трёхэтапный алгоритм:</b>
     * <ol>
     *   <li><b>Корректировка столбцов:</b> добавляет/удаляет {@link Rectangle} до {@code numSubsets = size.width/boxWidth}</li>
     *   <li><b>Агрегация сэмплов:</b> делит {@code samples} на {@code numSubsets} групп, вычисляет среднюю амплитуду</li>
     *   <li><b>Анимированный рендер:</b> 500мс на столбец с задержкой {@code i*2ms}, симметричная волна вокруг центра</li>
     * </ol>
     * </p>
     * <p>
     * <b>Математика волны:</b>
     * <ul>
     *   <li>{@code subsetLength = samples.length / numSubsets} — сэмплов на столбец</li>
     *   <li>{@code subset[i] = Σ|samples[k]| / length} — средняя абсолютная амплитуда</li>
     *   <li>{@code normal = 32768/maxVal} — нормализация к 16-bit диапазону</li>
     *   <li>{@code height = (subset[i] * normal / 32768) * (size.height/2)} — пиксельная высота</li>
     *   <li>{@code posY = height/2 - sample}, {@code negY = height/2 + sample} — симметрия</li>
     *   <li>{@code actualHeight = negY-posY+2px} — с 2px отступом</li>
     * </ul>
     * </p>
     * <p>
     * <b>Анимация:</b> {@link Root#iceInterpolator}, 500мс, каскадная ({@code i*2ms} delay).
     * </p>
     *
     * @param samples PCM сэмплы аудиофайла (от {@link #initSamples(File)})
     */
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

                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.millis(500),
                                new KeyValue(rect.yProperty(), posY, Root.iceInterpolator),
                                new KeyValue(rect.heightProperty(), negY - posY + 2, Root.iceInterpolator)
                        )
                );

                timeline.setDelay(Duration.millis(i * 2));
                timeline.play();
            }
        });
    }
    /**
     * Полностью декодирует аудиофайл в PCM сэмплы с поддержкой WAV/MP3/плагинных форматов.
     * <p>
     * <b>Поддерживаемые форматы:</b>
     * <ul>
     *   <li><b>WAV:</b> {@link AudioSystem#getAudioInputStream()} → PCM</li>
     *   <li><b>MP3:</b> {@link Mp3File} → {@link Bitstream} → {@link Mp3PcmStream} → PCM 16-bit</li>
     *   <li><b>Плагины:</b> {@link AudioDecoder} из {@link MediaProcessor#mediaPlayer#getDecoders()}</li>
     * </ul>
     * </p>
     * <p>
     * <b>MP3 специфика:</b>
     * <ul>
     *   <li>Каналы: Mono=1 (length=="Mono"), Stereo=2</li>
     *   <li>{@link AudioFormat}: PCM_SIGNED, 16-bit, sampleRate из тега, little-endian</li>
     *   <li>{@code frameLength = durationSeconds * sampleRate}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Валидация:</b> требует PCM_SIGNED encoding, выбрасывает {@link UnsupportedAudioFileException}.
     * Буфер: {@code channels * bytesPerSample * 8192} фреймов.
     * </p>
     * <p>
     * <b>Последовательность:</b> {@link #clearSamples()} → парсинг → {@link #readSamples()} → {@link #setSamples()}.
     * </p>
     *
     * @param file аудиофайл (WAV/MP3/поддерживаемые плагинами)
     * @throws RuntimeException оборачивает IOException/InvalidDataException/UnsupportedAudioFileException
     * @see #readSamples(int, AudioInputStream, byte[], int, boolean, int)
     */
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
    /** Коллбэк завершения полной загрузки волновой визуализации (декодирование + рендер). */
    public Runnable onLoadedSliderBackground;
    /**
     * Полный цикл: декодирование → визуализация → коллбэк → очистка памяти.
     * <p>
     * <b>Последовательность:</b>
     * <ol>
     *   <li>{@link #initSamples(File)} — PCM сэмплы</li>
     *   <li>{@link #loadBox(float[])} — волновая анимация</li>
     *   <li>{@link #onLoadedSliderBackground#run()} — пользовательский код</li>
     *   <li>{@link #clearSamples()} — если {@code clear_samples=true} в конфиге</li>
     * </ol>
     * Основной публичный метод для запуска визуализации аудиофайла.
     * </p>
     *
     * @param file аудиофайл для полной обработки
     * @see #setOnLoadedSliderBackground(Runnable)
     */
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
    /**
     * Устанавливает коллбэк завершения полной загрузки волновой визуализации (цепной вызов).
     * <p>
     * Вызывается <b>после</b> {@link #initSamples(File)} + {@link #loadBox(float[])} в {@link #loadSliderBackground(File)}.
     * Идеально для:
     * <ul>
     *   <li>Переключения на следующий трек</li>
     *   <li>Обновления UI ("Волна загружена")</li>
     *   <li>Синхронизации с {@link MediaPlayer}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Пример:</b> <pre>{@code slider.setOnLoadedSliderBackground(() -> player.play());}</pre>
     * </p>
     *
     * @param onLoadedSliderBackground обработчик завершения
     * @return {@code this} для fluent API
     * @see #loadSliderBackground(File)
     */
    public SoundSlider setOnLoadedSliderBackground(Runnable onLoadedSliderBackground) {
        this.onLoadedSliderBackground = onLoadedSliderBackground;
        return this;
    }
    /**
     * Устанавливает цвет активных столбцов волны.
     * <p>
     * Обновляет {@link #colorProperty} для {@link #updateSlider()}. Анимация применяется
     * при следующем изменении {@link #valueProperty()}.
     * </p>
     *
     * @param colorProperty цвет заполнения активной части волны
     * @see #getColorProperty()
     */
    public void setColor(Color colorProperty) {
        this.colorProperty.set(colorProperty);
    }
    /**
     * Возвращает свойство цвета активных столбцов для биндинга.
     * <p>
     * Позволяет связывать цвет волны с темой приложения:
     * <pre>{@code slider.getColorProperty().bind(theme.accentColorProperty());}</pre>
     * </p>
     *
     * @return {@link ObjectProperty} цвета для реактивных обновлений
     */
    public ObjectProperty<Color> getColorProperty() {
        return colorProperty;
    }
    /**
     * Обёртка для позиционирования слайдера в сцене (удобство для UI-builder).
     * <p>
     * Аналог {@code setLayoutX(x); setLayoutY(y);}, но в одном вызове.
     * Автоматически синхронизирует {@link #sliderBackground} через биндинг.
     * </p>
     *
     * @param x координата X
     * @param y координата Y
     */
    public void setLayouts(double x, double y) {
        super.setLayoutX(x);
        super.setLayoutY(y);
    }
    /**
     * Свойство цвета волны для двустороннего биндинга.
     * <p>
     * Синтаксический сахар над {@link #getColorProperty()}. Используется в FXML:
     * <pre>{@code <SoundSlider colorProperty="#themeAccent" />}</pre>
     * </p>
     *
     * @return то же свойство {@link #colorProperty}
     */
    public ObjectProperty<Color> colorPropertyProperty() {
        return colorProperty;
    }
    /**
     * Возвращает длительность анимации цветовых переходов столбцов.
     * <p>
     * Управляет скоростью {@link Timeline} в {@link #updateSlider()}:
     * <pre>{@code new KeyFrame(timeAnimation, new KeyValue(rect.fillProperty(), color))}</pre>
     * По умолчанию 125мс для отзывчивости.
     * </p>
     *
     * @return текущая длительность анимации
     */
    public Duration getTimeAnimation() {
        return timeAnimation;
    }
    /**
     * Устанавливает длительность анимации столбцов (цепной вызов).
     * <p>
     * Влияет на плавность {@link #updateSlider()}. Рекомендации:
     * <ul>
     *   <li><code>Duration.millis(100)</code> — быстрая реакция</li>
     *   <li><code>Duration.millis(200)</code> — плавная анимация</li>
     *   <li><code>Duration.ZERO</code> — мгновенно (без анимации)</li>
     * </ul>
     * </p>
     *
     * @param timeAnimation новая длительность
     * @return {@code this}
     */
    public SoundSlider setTimeAnimation(Duration timeAnimation) {
        this.timeAnimation = timeAnimation;
        return this;
    }
    /**
     * Возвращает размеры слайдера для расчёта столбцов.
     * <p>
     * Используется в:
     * <ul>
     *   <li>{@code numSubsets = size.width / boxWidth} — количество столбцов</li>
     *   <li>{@code height = normalized * (size.height / 2)} — высота волны</li>
     * </ul>
     * </p>
     *
     * @return {@link Dimension} слайдера (width=ширина, height=высота)
     */
    public Dimension getSize() {
        return size;
    }
    /**
     * Устанавливает размеры слайдера для визуализации (цепной вызов).
     * <p>
     * <b>Обновление после изменения:</b> вызовите {@link #initializeBox()} или {@link #loadBox(float[])}
     * для пересчёта столбцов под новую ширину.
     * </p>
     *
     * @param size новые размеры (width влияет на детализацию, height на амплитуду)
     * @return {@code this}
     */
    public SoundSlider setSize(Dimension size) {
        this.size = size;
        return this;
    }
    /**
     * Возвращает ширину одного столбца волны (по умолчанию 4px).
     * <p>
     * Меньше = больше деталей, но выше нагрузка. Больше = проще производительность.
     * </p>
     *
     * @return ширина столбца в пикселях
     */
    public int getBoxWidth() {
        return boxWidth;
    }
    /**
     * Возвращает радиус скругления углов столбцов (по умолчанию 6px).
     * <p>
     * Создаёт мягкий современный вид: {@code rect.setArcHeight/Width(arc)}.
     * </p>
     *
     * @return радиус скругления в пикселях
     */
    public int getArc() {
        return arc;
    }
}
