package rf.ebanina.ebanina.Player.AudioEffect.Tempo;

/**
 * <h1>TempoShifter</h1>
 * Реализация интерфейса {@link ITempoShifter} на основе <b>кубической интерполяции</b>.
 * <p>
 * Выполняет изменение темпа аудио в реальном времени с использованием высококачественной
 * <b>кубической интерполяции Catmull-Rom</b>. Алгоритм обеспечивает плавные переходы
 * между сэмплами и минимальные артефакты при изменении темпа до ±200%.
 * </p>
 *
 * <h3>Принцип работы</h3>
 * <p>
 * Алгоритм представляет собой <b>линейное растяжение времени</b> с последующей кубической
 * интерполяцией для сглаживания. Для каждого выходного сэмпла:
 * </p>
 * <ol>
 *   <li>Вычисляется позиция в исходном буфере: <code>srcIndex = i * tempo</code></li>
 *   <li>Берется 4 соседних сэмпла: <code>s[i-1], s[i], s[i+1], s[i+2]</code></li>
 *   <li>Применяется кубическая интерполяция Catmull-Rom</li>
 * </ol>
 *
 * <h3>Формула кубической интерполяции</h3>
 * <pre>{@code
 * P(t) = 0.5 * ( (2*y1) +
 *                (-y0 + y2) * t +
 *                (3*y0 - 3*y2 + 2*y1 - y3) * t² +
 *                (-2*y0 + 2*y2 - y1 + y3) * t³ )
 *
 * где t ∈ [0,1], y0..y3 — соседние сэмплы
 * }</pre>
 *
 * <h3>Преимущества реализации</h3>
 * <ul>
 *   <li><b>Высокое качество</b>: кубическая интерполяция лучше линейной</li>
 *   <li><b>Многоканальность</b>: синхронная обработка всех каналов</li>
 * </ul>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * ITempoShifter shifter = new TempoShifter();
 * float[][] result = shifter.applyTempo(
 *     stereoInput,  // 2 канала, 1024 сэмпла
 *     1024,
 *     2,
 *     1.25f         // +25% темпа
 * );
 * // result: 2 канала, ~819 сэмплов
 * }</pre>
 *
 * <h3>Ограничения</h3>
 * <ul>
 *   <li>Экстремальные значения <code>tempo</code> (&gt;3.0 или &lt;0.33) могут
 *       давать артефакты из-за недостатка сэмплов для интерполяции</li>
 *   <li>Задержка обработки пропорциональна <code>newFrames</code></li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 1.4.9
 * @see ITempoShifter
 * @implements ITempoShifter
 */
public class TempoShifter
        implements ITempoShifter
{
    private static final boolean isNativeProcessing = true;

    /**
     * {@inheritDoc}
     * <p>
     * <b>Алгоритм обработки:</b>
     * </p>
     * <ol>
     *   <li>Вычисляется количество выходных сэмплов: <code>newFrames = frames / tempo</code></li>
     *   <li>Для каждого канала и каждого выходного сэмпла:
     *     <ul>
     *       <li>Определяется дробная позиция в исходном буфере</li>
     *       <li>Берутся 4 соседних сэмпла с защитой от выхода за границы</li>
     *       <li>Применяется кубическая интерполяция</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * <p><b>Сложность:</b> O(channels × newFrames × 4) = линейная</p>
     */
    @Override
    public float[][] applyTempo(float[][] input, int frames, int channels, float tempo) {
        if(!isNativeProcessing) {
            int newFrames = Math.round(frames / tempo);
            float[][] output = new float[channels][newFrames];

            for (int ch = 0; ch < channels; ch++) {
                for (int i = 0; i < newFrames; i++) {
                    float srcIndex = i * tempo;
                    int i0 = (int) Math.floor(srcIndex) - 1;
                    float t = srcIndex - (i0 + 1);

                    float s0 = getSampleSafe(input[ch], i0, frames);
                    float s1 = getSampleSafe(input[ch], i0 + 1, frames);
                    float s2 = getSampleSafe(input[ch], i0 + 2, frames);
                    float s3 = getSampleSafe(input[ch], i0 + 3, frames);

                    output[ch][i] = cubicInterpolate(s0, s1, s2, s3, t);
                }
            }

            return output;
        } else {
            return nativeShifter.applyTempo(input, frames, channels, tempo);
        }
    }
    /**
     * Безопасный доступ к сэмплу с обработкой граничных случаев.
     * <p>
     * Гарантирует отсутствие {@link ArrayIndexOutOfBoundsException}:
     * </p>
     * <ul>
     *   <li><code>index &lt; 0</code> → возвращает <code>data[0]</code></li>
     *   <li><code>index ≥ length</code> → возвращает <code>data[length-1]</code></li>
     *   <li><code>null</code> или пустой массив → возвращает <code>0.0f</code></li>
     * </ul>
     *
     * @param data аудио буфер канала
     * @param index запрашиваемый индекс сэмпла
     * @param length длина буфера для проверки
     * @return безопасное значение сэмпла (-1.0f...+1.0f)
     */
    private float getSampleSafe(float[] data, int index, int length) {
        if (data == null || data.length == 0)
            return 0f;
        if (index < 0)
            return data[0];
        int maxIndex = Math.min(length, data.length) - 1;
        if (index > maxIndex)
            return data[maxIndex];
        return data[index];
    }
    /**
     * Кубическая интерполяция Catmull-Rom между 4 соседними сэмплами.
     * <p>
     * Использует полином 3-й степени для сглаженной интерполяции:
     * </p>
     * <pre>{@code
     * P(t) = a0*t³ + a1*t² + a2*t + a3
     *
     * a0 = y3 - y2 - y0 + y1
     * a1 = y0 - y1 - a0
     * a2 = y2 - y0
     * a3 = y1
     * }</pre>
     *
     * <p>Где <code>t ∈ [0,1]</code> — дробная часть позиции сэмпла.</p>
     *
     * @param y0 сэмпл <code>i-1</code> (слева)
     * @param y1 сэмпл <code>i</code> (левый опорный)
     * @param y2 сэмпл <code>i+1</code> (правый опорный)
     * @param y3 сэмпл <code>i+2</code> (справа)
     * @param t вес интерполяции [0.0f...1.0f]
     * @return интерполированное значение сэмпла
     */
    private float cubicInterpolate(float y0, float y1, float y2, float y3, float t) {
        float a0 = y3 - y2 - y0 + y1;
        float a1 = y0 - y1 - a0;
        float a2 = y2 - y0;
        float a3 = y1;

        return (a0 * t * t * t) + (a1 * t * t) + (a2 * t) + a3;
    }

    protected static NativeTempoShifter nativeShifter = new NativeTempoShifter();
}
