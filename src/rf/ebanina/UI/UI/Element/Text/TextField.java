package rf.ebanina.UI.UI.Element.Text;

import javafx.animation.FillTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

/**
 * <h1>TextField</h1>
 * Кастомный текстовый поле с расширенными возможностями анимации, динамической стилизацией и адаптивным выравниванием текста.
 * <p>
 * Этот класс расширяет стандартный {@link javafx.scene.control.TextField}, добавляя:
 * <ul>
 *   <li><b>Динамическое изменение цвета</b> текста и выделения через {@link #colorPropertyProperty()}.</li>
 *   <li><b>Анимации фокуса</b> — плавное изменение цвета текста при получении/потере фокуса.</li>
 *   <li><b>Ховер-эффекты</b> — масштабирование при наведении мыши.</li>
 *   <li><b>Адаптивное выравнивание</b> — текст центрируется, если помещается полностью, иначе выравнивается по левому краю.</li>
 *   <li><b>Прозрачный фон</b> с кастомными цветами выделения, вычисляемыми на основе базового цвета.</li>
 * </ul>
 * </p>
 * <p>
 * Класс предназначен для создания современных UI-элементов с Material Design-подобными эффектами.
 * Все анимации используют короткие длительности (150-500мс) для отзывчивости интерфейса.
 * </p>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * TextField textField = new TextField("Пример текста");
 * textField.setFontSize(14);
 * textField.setColorProperty(Color.BLUE);
 *
 * // Динамическое изменение цвета
 * textField.colorPropertyProperty().set(Color.RED);
 * }</pre>
 *
 * <h3>Особенности поведения</h3>
 * <ul>
 *   <li>При пустом тексте — всегда центрированное выравнивание.</li>
 *   <li>Цвет выделения автоматически вычисляется как затемнённая версия базового цвета.</li>
 *   <li>Все обновления UI происходят через {@link Platform#runLater(Runnable)} ()} для потокобезопасности.</li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 0.1.4.4
 * @see javafx.scene.control.TextField
 * @see ColorProcessor
 * @see ResourceManager
 */
public class TextField
        extends javafx.scene.control.TextField
{
    /**
     * Свойство для динамического управления цветом текста и выделения.
     * <p>
     * При изменении значения свойства автоматически:
     * <ul>
     *   <li>Обновляются CSS-стили выделения через {@link #updateSelectionColors(Color)}.</li>
     *   <li>Обновляется цвет текста через {@link #updateColor(Color)}.</li>
     * </ul>
     * </p>
     * <p>
     * По умолчанию <code>null</code> (используется {@link Color#BLACK}).
     * </p>
     *
     * @return свойство цвета
     * @see #setColorProperty(Color)
     * @see #updateColor(Color)
     */
    private ObjectProperty<Color> colorProperty;

    /**
     * Слушатель изменений цвета, автоматически регистрируется в конструкторе.
     * <p>
     * Реагирует на изменения {@link #colorProperty} и запускает цепочку обновлений UI.
     * </p>
     *
     * @see #colorProperty
     */
    private ChangeListener<Color> colorListener;

    /**
     * Создаёт пустое текстовое поле с настройками по умолчанию.
     * <p>
     * Автоматически вызывает {@link #TextField(String)} с пустой строкой.
     * </p>
     */
    public TextField() {
        this("");
    }

    /**
     * Создаёт текстовое поле с заданным начальным текстом.
     * <p>
     * Инициализирует все эффекты и слушатели:
     * <ul>
     *   <li>Шрифт размером 11pt из {@link ResourceManager}.</li>
     *   <li>Центрированное выравнивание.</li>
     *   <li>Прозрачный фон.</li>
     *   <li>Слушатели текста, ширины, фокуса, мыши.</li>
     *   <li>Анимацию появления (fade-in 500мс).</li>
     * </ul>
     * </p>
     *
     * @param text начальный текст поля
     */
    public TextField(String text) {
        super(text);

        setFontSize(11);
        setPadding(new Insets(0));
        setAlignment(Pos.CENTER);
        setBackground(Background.EMPTY);
        setFocusTraversable(true);

        colorProperty = new SimpleObjectProperty<>();
        colorProperty.addListener(colorListener = (obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateSelectionColors(newVal);
                Platform.runLater(() -> updateColor(newVal));
            }
        });

        textProperty().addListener((obs, oldText, newText) -> updateAlignment());
        widthProperty().addListener((obs, oldWidth, newWidth) -> updateAlignment());

        focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            Node textNode = lookup(".text");
            if (textNode instanceof Text t) {
                Color nativeColor = colorProperty.get() != null ? colorProperty.get() : Color.BLACK;

                Color highlightColor = (nativeColor.equals(Color.BLACK) || nativeColor.getBrightness() < 0.1)
                        ? Color.WHITE
                        : nativeColor.deriveColor(0, 1.0, 1.5, 1.0);

                FillTransition ft = new FillTransition(Duration.millis(200), t);
                ft.setToValue(isFocused ? highlightColor : nativeColor);
                ft.play();
            }
        });

        setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), this);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });

        setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), this);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        Platform.runLater(() -> {
            javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(Duration.millis(500), this);
            fade.setToValue(1.0);
            fade.play();
        });
    }

    /**
     * Обновляет CSS-стили выделения текста на основе базового цвета.
     * <p>
     * Вычисляет полупрозрачный цвет выделения как <code>baseColor.deriveColor(0, 0.8, 0.6, 0.5)</code>
     * и применяет его к свойствам <code>-fx-selection-bar</code> и <code>-fx-highlight-fill</code>.
     * </p>
     *
     * @param baseColor базовый цвет для вычисления оттенков выделения
     * @see ColorProcessor#core
     */
    private void updateSelectionColors(Color baseColor) {
        Color selectionColor = baseColor.deriveColor(0, 0.8, 0.6, 0.5);

        String hexSelection = ColorProcessor.core.toHex(selectionColor);

        setStyle(String.format(
                "-fx-background-color: transparent; " +
                        "-fx-selection-bar: %s; " +
                        "-fx-selection-fill: white; " +
                        "-fx-highlight-fill: %s;",
                hexSelection, hexSelection
        ));
    }

    /**
     * Возвращает свойство цвета для привязки.
     *
     * @return ObjectProperty&lt;Color&gt; для динамического изменения цвета
     */
    public ObjectProperty<Color> colorPropertyProperty() {
        return colorProperty;
    }

    /**
     * Устанавливает базовый цвет текста и выделения.
     * <p>
     * Автоматически срабатывает слушатель {@link #colorListener}, обновляя UI.
     * </p>
     *
     * @param colorProperty новый цвет
     * @see #colorPropertyProperty()
     */
    public void setColorProperty(Color colorProperty) {
        this.colorProperty.set(colorProperty);
    }

    /**
     * Возвращает текущий слушатель изменений цвета.
     *
     * @return зарегистрированный ChangeListener&lt;Color&gt;
     */
    public ChangeListener<Color> getColorListener() {
        return colorListener;
    }

    /**
     * Заменяет слушатель изменений цвета.
     * <p>
     * Предыдущий слушатель удаляется автоматически при установке нового.
     * </p>
     *
     * @param colorListener новый слушатель
     * @return this для цепочки вызовов (builder pattern)
     */
    public TextField setColorListener(ChangeListener<Color> colorListener) {
        this.colorListener = colorListener;
        return this;
    }

    /**
     * Динамически обновляет выравнивание текста в зависимости от его длины.
     * <p>
     * Логика:
     * <ul>
     *   <li>Если текст пустой — {@link Pos#CENTER}.</li>
     *   <li>Если ширина текста ≤ ширине поля — {@link Pos#CENTER}.</li>
     *   <li>Иначе — {@link Pos#CENTER_LEFT}.</li>
     * </ul>
     * Выполняется асинхронно через {@link Platform#runLater(Runnable)} ()}.
     * </p>
     */
    private void updateAlignment() {
        Platform.runLater(() -> {
            String text = getText();
            if (text == null || text.isEmpty()) {
                setAlignment(Pos.CENTER);
                return;
            }

            Text tempText = new Text(text);
            tempText.setFont(getFont());
            double textWidth = tempText.getLayoutBounds().getWidth();
            double fieldWidth = getWidth() - getPadding().getLeft() - getPadding().getRight();

            setAlignment(textWidth <= fieldWidth ? Pos.CENTER : Pos.CENTER_LEFT);
        });
    }

    /**
     * Возвращает текущее свойство цвета.
     *
     * @return ObjectProperty&lt;Color&gt;
     * @see #colorPropertyProperty()
     */
    public ObjectProperty<Color> getColorProperty() {
        return colorProperty;
    }

    /**
     * Устанавливает размер шрифта из ресурсов приложения.
     * <p>
     * Загружает шрифт <code>"main_font"</code> указанного размера через {@link ResourceManager#Instance}.
     * </p>
     *
     * @param size размер шрифта в пунктах
     * @see ResourceManager#loadFont(String, int)
     */
    public void setFontSize(int size) {
        super.setFont(ResourceManager.Instance.loadFont("main_font", size));
    }

    /**
     * Регистрирует обработчик перетаскивания мышью для перемещения поля.
     * <p>
     * Добавляет {@link MouseEvent#MOUSE_DRAGGED} обработчик, который вызывает
     * {@link #setLayouts(double, double)} с координатами относительно сцены.
     * </p>
     */
    public void replace() {
        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> setLayouts(event.getSceneX() + getPrefWidth() / 2, event.getSceneY() + getPrefHeight() / 2));
    }

    /**
     * Обновляет цвет текста в UI.
     * <p>
     * Ищет внутренний узел <code>.text</code>, разбиндит его свойство <code>fill</code>
     * и устанавливает новый цвет в HEX-формате через {@link ColorProcessor#core#toHex(Color)}.
     * </p>
     * <p>
     * Выполняется асинхронно. Бросает {@link NullPointerException}, если узел не найден.
     * </p>
     *
     * @param color новый цвет текста; если <code>null</code> — метод завершается
     * @throws NullPointerException если внутренний Text-узел не найден
     */
    public void updateColor(Color color) {
        if (color == null)
            return;

        String colorHex = ColorProcessor.core.toHex(color);

        Platform.runLater(() -> {
            Node textNode = lookup(".text");

            if(textNode != null) {
                if (textNode instanceof Text text) {
                    if (text.getFill() instanceof Color) {
                        if(text.fillProperty().isBound()) {
                            text.fillProperty().unbind();
                        }

                        text.setFill(Color.valueOf(colorHex));
                    }
                }
            } else {
                throw new NullPointerException("lookup(\".text\") not can be null");
            }
        });
    }

    /**
     * Устанавливает абсолютные координаты поля на сцене.
     * <p>
     * Используется для программного перемещения элемента (drag & drop).
     * </p>
     *
     * @param x координата X
     * @param y координата Y
     * @see #replace()
     */
    public void setLayouts(double x, double y) {
        setLayoutX(x);
        setLayoutY(y);
    }
}
