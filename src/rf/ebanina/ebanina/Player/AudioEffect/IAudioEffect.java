package rf.ebanina.ebanina.Player.AudioEffect;

import javafx.stage.Stage;

import java.util.Map;

/**
 * Интерфейс IAudioEffect представляет аудиоэффект для использования в аудиоплеере.
 * <p>
 * Этот интерфейс описывает базовые методы для реализации звукового эффекта, который может
 * обрабатываться аудиоплеером. Он поддерживает получение имени эффекта, активацию/деактивацию,
 * открытие графического редактора эффекта и обработку аудиоданных.
 * </p>
 * <p>
 * Все звуковые данные подаются в метод {@link #process(float[][], int)} в виде двумерного массива
 * с сэмплами, где первый индекс — канал, а второй — фрейм. Возвращается также двумерный массив,
 * представляющий преобразованные аудиоданные.
 * </p>
 *
 * <h2>Пример использования</h2>
 * <pre>{@code
 * public class EchoEffect implements IAudioEffect {
 *     private boolean active = true;
 *     private String name = "Echo Effect";
 *
 *     @Override
 *     public String getName() {
 *         return name;
 *     }
 *
 *     @Override
 *     public void setActive(boolean isActive) {
 *         this.active = isActive;
 *     }
 *
 *     @Override
 *     public boolean isActive() {
 *         return active;
 *     }
 *
 *     @Override
 *     public void openEditor(Stage parent) {
 *         // Реализация открытия редактора эффекта через JavaFX Stage
 *     }
 *
 *     @Override
 *     public float[][] process(float[][] in, int frames) {
 *         // Пример простой обработки: возврат данных без изменений
 *         return in;
 *     }
 * }
 * }</pre>
 *
 * @author Ebanina Std.
 * @version 0.1.4.6
 * @since 0.1.4
 * @see javafx.stage.Stage
 */
public interface IAudioEffect {
    /**
     * Получить имя аудиоэффекта.
     * @return имя эффекта
     */
    String getName();

    /**
     * Активировать или деактивировать эффект.
     * @param isActive true для активации, false для деактивации
     */
    void setActive(boolean isActive);

    /**
     * Проверить, активен ли эффект.
     * @return true, если эффект активен
     */
    boolean isActive();

    /**
     * Открыть графический редактор эффекта.
     * @param parent родительское окно JavaFX Stage
     */
    void openEditor(Stage parent);

    /**
     * Обработать аудиоданные.
     * @param in входные аудиоданные [канал][фрейм]
     * @param frames количество фреймов для обработки
     * @return обработанные аудиоданные того же формата
     */
    float[][] process(float[][] in, int frames);

    Map<String, String> load();
}