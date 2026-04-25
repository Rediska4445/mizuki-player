package rf.ebanina.UI.UI.Element.Text;

import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.Text.Skins.FadeTextSkin;

/**
 * Расширенная надпись с поддержкой перетаскивания мышью и fluent API.
 * <p>
 * Компонент наследует {@link javafx.scene.control.Label} и добавляет:
 * <ul>
 *   <li>Автозагрузка шрифта {@code "main_font"} (12px) из {@link ResourceManager}.</li>
 *   <li>Fluent-метод {@link #setColor(Color)} для цепных вызовов.</li>
 *   <li>Перетаскивание мышью через {@link #replace()} (центрируется по курсору).</li>
 * </ul>
 * </p>
 *
 * <h3>Перетаскивание</h3>
 * <ul>
 *   <li>Активируется вызовом {@link #replace()}.</li>
 *   <li>Label центрируется по курсору ({@code sceneX + width/2}, {@code sceneY + height/2}).</li>
 *   <li>Работает только {@link MouseEvent#MOUSE_DRAGGED}.</li>
 * </ul>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * // Простая цветная надпись
 * Label title = new Label("Play", Color.WHITE).setColor(Color.CYAN);
 *
 * // Перетаскиваемая надпись с позиционированием
 * Label draggable = new Label("Drag me", Color.YELLOW, 100, 50);
 * draggable.replace(); // Включаем перетаскивание
 *
 * // Минимальный конструктор
 * Label simple = new Label("Default black text");
 * }</pre>
 *
 * Идеально подходит для UI-builder'ов, прототипирования интерфейсов и
 * дизайнерских инструментов с WYSIWYG редактированием текста.
 *
 * @author Ebanina Std
 * @since 0.1.4.4
 * @see javafx.scene.control.Label
 * @see ResourceManager
 */
public class Label
        extends javafx.scene.control.Label
{
    /**
     * Создаёт надпись с чёрным текстом и стандартным шрифтом.
     */
    public Label() {
        this(null);
    }
    /**
     * Создаёт надпись с заданным текстом (чёрный цвет, стандартный шрифт).
     *
     * @param text текст надписи
     */
    public Label(String text) {
        this(text, Color.BLACK);
    }
    /**
     * Создаёт надпись с текстом, цветом и позицией.
     * <p>
     * Автоматически применяет:
     * <ul>
     *   <li>Шрифт {@code "main_font"} размером 12px.</li>
     *   <li>Позиционирование через {@link #setLayouts(double, double)}.</li>
     * </ul>
     * </p>
     *
     * @param text текст надписи
     * @param color цвет текста
     * @param X координата X
     * @param Y координата Y
     */
    public Label(String text, Color color, double X, double Y) {
        super(text);
        super.setTextFill(color);
        super.setFont(ResourceManager.Instance.loadFont("main_font", 12));

        setSkin(new FadeTextSkin(this));

        setLayouts(X, Y);
    }
    /**
     * Создаёт надпись с текстом и цветом (позиция 0,0).
     *
     * @param text текст надписи
     * @param color цвет текста
     */
    public Label(String text, Color color) {
        this(text, color, 0, 0);
    }
    /**
     * Активирует режим перетаскивания мышью.
     * <p>
     * Добавляет обработчик {@link MouseEvent#MOUSE_DRAGGED}, который центрирует
     * надпись по курсору: <code>layoutX = sceneX + width/2</code>,
     * <code>layoutY = sceneY + height/2</code>.
     * </p>
     */
    public void replace() {
        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> setLayouts(event.getSceneX() + getPrefWidth() / 2, event.getSceneY() + getPrefHeight() / 2));
    }
    /**
     * Устанавливает цвет текста (цепной вызов).
     * <p>
     * Fluent API для удобной стилизации:
     * <pre>{@code new Label("Text").setColor(Color.RED).setColor(Color.BLUE);}</pre>
     * </p>
     *
     * @param c новый цвет текста
     * @return {@code this} для цепных вызовов
     */
    public Label setColor(Color c) {
        setTextFill(c);

        return this;
    }
    /**
     * Устанавливает позицию надписи в сцене.
     * <p>
     * Обёртка над {@link #setLayoutX(double)} и {@link #setLayoutY(double)}
     * для удобства перетаскивания.
     * </p>
     *
     * @param x координата X
     * @param y координата Y
     */
    public void setLayouts(double x, double y) {
        super.setLayoutX(x);
        super.setLayoutY(y);
    }
}
