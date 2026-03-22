package rf.ebanina.ebanina.Player.AudioEffect.Plugins;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import rf.ebanina.ebanina.Player.AudioEffect.IAudioEffect;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Стерео панорамирующий эффект (Panix) для аудиоплеера.
 * <p>
 * Создает эффект панорамирования между левым и правым каналами,
 * изменяя баланс громкости каналов по синусоидальной кривой.
 * Предназначен для создания пространственных эффектов и стерео-расширения.
 * </p>
 *
 * <h3>Диапазон панорамы:</h3>
 * <ul>
 *   <li><strong>-1.0</strong> = полностью правый канал</li>
 *   <li><strong>0.0</strong> = центр (равная громкость L/R)</li>
 *   <li><strong>+1.0</strong> = полностью левый канал</li>
 * </ul>
 *
 * <h3>Формула панорамирования:</h3>
 * <pre>{@code
 * leftMul  = cos((pan + 1) * π/4)
 * rightMul = cos((1 - pan) * π/4)
 * }</pre>
 *
 * <p>Косинусная кривая обеспечивает плавный переход без "дыр" в середине.</p>
 *
 * <h3>Применение:</h3>
 * <ul>
 *   <li>Создание эффекта движения звука L→R или R→L</li>
 *   <li>Стерео-расширение инструментов</li>
 *   <li>Позиционирование вокала/инструментов в панораме</li>
 * </ul>
 *
 * @author Ebanina
 * @version 1.0
 * @since 1.0
 */
public class Panix implements IAudioEffect {
    /** Текущее значение панорамы (-1.0...+1.0), потокобезопасное */
    private AtomicReference<Float> currentPan = new AtomicReference<>(0.0f);
    /** Состояние активности эффекта */
    private boolean isActive = true;
    /**
     * Возвращает название плагина для интерфейса.
     *
     * @return "Panix"
     */
    @Override
    public String getName() {
        return "Panix";
    }
    /**
     * Включает/выключает эффект панорамирования.
     *
     * @param isActive true - эффект активен, false - байпас (оригинальный сигнал)
     */
    @Override
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    /**
     * Проверяет активность эффекта.
     *
     * @return true если панорамирование активно
     */
    @Override
    public boolean isActive() {
        return isActive;
    }
    /** Слайдер панорамы для GUI */
    private Slider panSlider;
    /** Метка отображения текущего значения панорамы */
    private Label panLabel;
    /**
     * Открывает редактор настроек панорамирования.
     *
     * @param parent родительское окно JavaFX Stage
     */
    @Override
    public void openEditor(Stage parent) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        panLabel = new Label("Pan: " + String.format("%.2f", currentPan.get()) + "x");

        panSlider = new Slider(-1, 1, currentPan.get());
        panSlider.setShowTickLabels(true);
        panSlider.setShowTickMarks(true);
        panSlider.setMajorTickUnit(0.5);
        panSlider.setMinorTickCount(5);
        panSlider.setBlockIncrement(0.1);

        panSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentPan.set(newValue.floatValue());
            panLabel.setText("Pan: " + String.format("%.2f", currentPan.get()) + "x");
        });

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> parent.close());

        root.getChildren().addAll(
                new Label("Pan"),
                panLabel,
                panSlider,
                closeButton
        );

        Scene scene = new Scene(root, 300, 150);
        parent.setTitle("Panix (Ebanina Std)");
        parent.setScene(scene);
        parent.setResizable(false);
        parent.show();
    }
    /**
     * Обрабатывает стерео аудио буфер, применяя панорамирование.
     * Работает только со стерео (2 канала).
     *
     * @param in входной аудио буфер (L, R, ...)
     * @param frames количество сэмплов в каждом канале
     * @return панорамированный стерео буфер
     */
    @Override
    public float[][] process(float[][] in, int frames) {
        if(isActive) {
            float[][] output = new float[in.length][frames];

            System.arraycopy(in, 0, output, 0, in.length);

            if (output.length >= 2) {
                float leftMul = (float) (Math.cos((currentPan.get() + 1) * Math.PI / 4));
                float rightMul = (float) (Math.cos((1 - currentPan.get()) * Math.PI / 4));

                for (int i = 0; i < output[0].length; i++) {
                    output[0][i] *= leftMul;
                    output[1][i] *= rightMul;
                }
            }

            return output;
        } else {
            return in;
        }
    }
    /**
     * Сохраняет настройки плагина в Map для персистентности.
     *
     * @return Map с параметрами:
     * <ul>
     *   <li>"plugin.panix.enable" - состояние активности</li>
     *   <li>"plugin.panix.pan" - текущее значение панорамы</li>
     * </ul>
     */
    @Override
    public Map<String, String> load() {
        return Map.of(
                "plugin.panix.enable", String.valueOf(isActive),
                "plugin.panix.pan", String.valueOf(currentPan.get())
        );
    }
}
