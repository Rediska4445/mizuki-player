package rf.ebanina.ebanina.Player.AudioEffect.Tempo;

import rf.ebanina.File.Resources.ResourceManager;

import java.io.File;

/**
 * Нативный процессор изменения темпа аудио через JNI.
 * <p>
 * Класс предоставляет высокопроизводительную реализацию алгоритма time-stretching
 * на основе кубической интерполяции. Обработка выполняется в нативном коде (C++),
 * что обеспечивает минимальные задержки и оптимальное использование памяти.
 * </p>
 * <p>
 * <b>Принцип работы:</b>
 * </p>
 * <ul>
 *   <li>При загрузке класса автоматически загружается DLL-библиотека {@code tempoShifter.dll}
 *       из директории, указанной в {@link ResourceManager#BIN_LIBRARIES_PATH}</li>
 *   <li>Метод {@link #applyTempo(float[][], int, int, float)} создаёт выходной массив
 *       и делегирует обработку нативному методу {@link #applyTempoNative}</li>
 *   <li>Нативная функция выполняет кубическую интерполяцию для каждого сэмпла,
 *       обеспечивая плавное изменение темпа без артефактов</li>
 * </ul>
 * <p>
 * <b>Требования:</b> наличие скомпилированной {@code tempoShifter.dll} в пути
 * {@code ResourceManager.BIN_LIBRARIES_PATH}. Библиотека должна быть скомпилирована
 * для той же архитектуры (x64/x86), что и JVM.
 * </p>
 *
 * @see ResourceManager#BIN_LIBRARIES_PATH
 * @since 1.4.9
 */
public class NativeTempoShifter {
    /**
     * Полный путь к нативной библиотеке {@code tempoShifter.dll}.
     * <p>
     * Формируется как {@code ResourceManager.BIN_LIBRARIES_PATH + File.separator + "tempoShifter.dll"}
     * </p>
     */
    public static final String TEMPO_SHIFTER_DLL = ResourceManager.BIN_LIBRARIES_PATH +
            File.separator + "tempoShifter.dll";

    /**
     * Статический блок инициализации — загружает нативную библиотеку.
     * <p>
     * Вызывается автоматически при первом обращении к классу.
     * Если библиотека не найдена или несовместима с архитектурой JVM,
     * будет выброшено {@link UnsatisfiedLinkError}.
     * </p>
     */
    static {
        System.load(TEMPO_SHIFTER_DLL);
    }
    /**
     * Нативный метод изменения темпа аудио.
     * <p>
     * Реализация на C++ обрабатывает входной массив {@code input} и записывает
     * результат в предварительно выделенный массив {@code output}.
     * </p>
     *
     * @param input    входные данные: 2D-массив {@code float[channels][frames]}
     * @param frames   количество входных сэмплов на канал
     * @param channels количество каналов
     * @param tempo    коэффициент темпа ({@code >1} — ускорение, {@code <1} — замедление)
     * @param output   выходной массив: {@code float[channels][newFrames]}, должен быть выделен заранее
     * @return количество выходных сэмплов ({@code newFrames})
     *
     * @see #applyTempo(float[][], int, int, float)
     */
    public native int applyTempoNative(float[][] input, int frames, int channels, float tempo, float[][] output);
    /**
     * Публичный API для изменения темпа аудио.
     * <p>
     * Автоматически вычисляет размер выходного буфера, создаёт его и вызывает
     * нативный метод {@link #applyTempoNative} для обработки.
     * </p>
     *
     * @param input    входные данные: 2D-массив {@code float[channels][frames]}
     * @param frames   количество входных сэмплов на канал
     * @param channels количество каналов
     * @param tempo    коэффициент темпа ({@code >1} — ускорение, {@code <1} — замедление)
     * @return выходной массив {@code float[channels][newFrames]} с обработанным аудио
     *
     * @implNote Сложность алгоритма: O(channels × newFrames × 4) — линейная
     * @see #applyTempoNative(float[][], int, int, float, float[][])
     */
    public float[][] applyTempo(float[][] input, int frames, int channels, float tempo) {
        int newFrames = Math.round(frames / tempo);
        float[][] output = new float[channels][newFrames];

        applyTempoNative(input, frames, channels, tempo, output);

        return output;
    }
}