package rf.ebanina.UI.UI.Element;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import rf.ebanina.UI.UI.Element.Buttons.Button;

/**
 * Анимированная нижняя панель управления плеером с авто-сворачиванием.
 * <p>
 * Компонент представляет собой плавающую панель в нижней части экрана, которая
 * автоматически сворачивается в тонкую полоску и разворачивается при наведении
 * мыши на зону hover. Содержит центральную кнопку действий и использует плавные
 * анимации высоты и прозрачности для премиум UX.
 * </p>
 *
 * <h3>Поведение</h3>
 * <ul>
 *   <li><b>Свернутое состояние</b>: высота = 2.8% от {@link #expandedHeight} (по умолчанию 80px × 0.035 ≈ 2.8px).</li>
 *   <li><b>Развёрнутое состояние</b>: полная высота 80px при наведении на hoverZone или саму панель.</li>
 *   <li><b>Hover-зона</b>: невидимая область шириной {@link #hoverDimension} × 2px по бокам панели, высотой 12.5% экрана.</li>
 *   <li><b>Анимации</b>: высота (400мс, back-ease-out), прозрачность кнопки (125мс). Использует custom SPLINE интерполятор.</li>
 * </ul>
 *
 * <h3>Визуальное оформление</h3>
 * <ul>
 *   <li>Полупрозрачный чёрный фон с закруглением только верха (10px).</li>
 *   <li>Серая рамка только по трём сторонам (без низа).</li>
 *   <li>Автопозиционирование по центру снизу экрана (35% ширины).</li>
 * </ul>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * ControlPane controlPane = new ControlPane(rootPane);
 * controlPane.addCenteredButton(new Button("Play"));
 * controlPane.setExpandedHeight(100);
 * controlPane.setAnimationColor(Color.CYAN);
 * }</pre>
 *
 * Панель автоматически добавляет hoverZone в родительский контейнер и управляет
 * всей логикой анимаций сворачивания/разворачивания без внешнего вмешательства.
 *
 * @author Ebanina Std
 * @since 0.1.4.4
 * @see Timeline
 * @see Interpolator
 * @see Button
 */
public class ControlPane
        extends Pane
{
    /**
     * Коэффициент высоты свёрнутого состояния относительно полной высоты.
     */
    protected double COLLAPSED_HEIGHT_RATIO = 0.035;
    /**
     * Полная высота развёрнутого состояния (по умолчанию 80px).
     */
    protected double expandedHeight = 80;
    /**
     * Размер боковых отступов невидимой hover-зоны.
     */
    protected int hoverDimension = 25;
    /**
     * Невидимая зона hover-активации, добавляемая в родительский контейнер.
     * <p>
     * Ширина = ширина панели + 2×hoverDimension, высота = 12.5% экрана, расположена снизу.
     * </p>
     */
    protected final Region hoverZone;
    /**
     * Центральная кнопка действий панели.
     */
    protected javafx.scene.control.Button mainButton;
    /**
     * Цвет анимаций, используемый для кастомизации визуального стиля.
     */
    protected final ObjectProperty<Color> animationColor = new SimpleObjectProperty<>(Color.LIGHTGRAY);
    /**
     * Создаёт панель управления, автоматически привязанную к родительскому контейнеру.
     * <p>
     * Инициализирует:
     * <ul>
     *   <li>Свернутое состояние (2.8px высота).</li>
     *   <li>Полупрозрачный фон и рамку с закруглением верха.</li>
     *   <li>Автопозиционирование по центру снизу (35% ширины).</li>
     *   <li>Невидимую hoverZone и обработчики событий мыши.</li>
     * </ul>
     * </p>
     *
     * @param root родительский контейнер для позиционирования и добавления hoverZone
     */
    public ControlPane(Pane root) {
        setPrefHeight(expandedHeight * COLLAPSED_HEIGHT_RATIO);

        setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.35),
                new CornerRadii(10, 10, 0, 0, false),
                Insets.EMPTY
        )));

        setBorder(new Border(new BorderStroke(
                Color.GRAY,
                BorderStrokeStyle.SOLID,
                new CornerRadii(10, 10, 0, 0, false),
                new BorderWidths(1,1,0,1)
        )));

        prefWidthProperty().bind(root.widthProperty().multiply(0.35));

        layoutXProperty().bind(root.widthProperty().subtract(widthProperty()).divide(2));
        layoutYProperty().bind(root.heightProperty().subtract(heightProperty()));

        hoverZone = new Region();

        hoverZone.prefHeightProperty().bind(root.heightProperty().multiply(0.125));
        hoverZone.prefWidthProperty().bind(prefWidthProperty().add(hoverDimension * 2));
        hoverZone.layoutXProperty().bind(layoutXProperty().subtract(hoverDimension));
        hoverZone.layoutYProperty().bind(root.heightProperty().subtract(hoverZone.prefHeightProperty()));

        hoverZone.toBack();

        hoverZone.setOnMouseEntered(e -> animateHeight(expandedHeight, () -> animateButtonOpacity(mainButton, mainButton.getOpacity(), 1, 125)));
        hoverZone.setOnMouseExited(e -> animateHeight(expandedHeight * COLLAPSED_HEIGHT_RATIO, () -> animateButtonOpacity(mainButton, mainButton.getOpacity(), 0, 125)));

        setOnMouseEntered(e -> animateHeight(expandedHeight, () -> animateButtonOpacity(mainButton, mainButton.getOpacity(), 1, 125)));
        setOnMouseExited(e -> animateHeight(expandedHeight * COLLAPSED_HEIGHT_RATIO, () -> animateButtonOpacity(mainButton, mainButton.getOpacity(), 0, 125)));

        if(root.getChildren().contains(this)) {
            root.getChildren().remove(hoverZone);
        }

        root.getChildren().add(hoverZone);
    }

    /**
     * Добавляет центральную кнопку действий и центрирует её.
     * <p>
     * Кнопка позиционируется по центру панели с вертикальным отступом 25% от высоты.
     * </p>
     *
     * @param button кнопка для добавления (станет {@link #mainButton})
     */
    public void addCenteredButton(javafx.scene.control.Button button) {
        if (!getChildren().contains(button)) {
            getChildren().add(mainButton = button);
        }

        button.layoutXProperty().bind(prefWidthProperty().multiply(0.5).subtract(button.prefWidthProperty().multiply(0.5)));
        button.layoutYProperty().bind(prefHeightProperty().multiply(0.5).subtract(prefHeightProperty().multiply(0.25)));
    }

    /**
     * Возвращает центральную кнопку панели.
     *
     * @return текущая mainButton или {@code null}
     */
    public javafx.scene.control.Button getMainButton() {
        return mainButton;
    }
    /**
     * Полная высота развёрнутого состояния.
     *
     * @return текущая высота в развёрнутом состоянии
     */
    public double getExpandedHeight() {
        return expandedHeight;
    }
    /**
     * Устанавливает полную высоту развёрнутого состояния (цепной вызов).
     *
     * @param expandedHeight новая высота
     * @return {@code this} для fluent API
     */
    public ControlPane setExpandedHeight(double expandedHeight) {
        this.expandedHeight = expandedHeight;
        return this;
    }
    /**
     * Возвращает цвет анимаций.
     *
     * @return текущий цвет анимаций
     */
    public Color getAnimationColor() {
        return animationColor.get();
    }
    /**
     * Устанавливает цвет анимаций.
     *
     * @param animationColor новый цвет
     */
    public void setAnimationColor(Color animationColor) {
        this.animationColor.set(animationColor);
    }
    /**
     * Возвращает невидимую hover-зону.
     *
     * @return объект {@link Region} для дополнительной настройки
     */
    public Region getHoverZone() {
        return hoverZone;
    }
    /**
     * Свойство цвета анимаций для биндинга.
     *
     * @return свойство цвета
     */
    public ObjectProperty<Color> animationColorProperty() {
        return animationColor;
    }
    /**
     * Размер боковых отступов hover-зоны.
     *
     * @return текущий размер в пикселях
     */
    public int getHoverDimension() {
        return hoverDimension;
    }
    /**
     * Устанавливает размер hover-зоны (цепной вызов).
     *
     * @param hoverDimension новая ширина отступов
     * @return {@code this}
     */
    public ControlPane setHoverDimension(int hoverDimension) {
        this.hoverDimension = hoverDimension;
        return this;
    }
    /**
     * Устанавливает центральную кнопку (цепной вызов).
     *
     * @param mainButton новая кнопка
     * @return {@code this}
     */
    public ControlPane setMainButton(Button mainButton) {
        this.mainButton = mainButton;
        return this;
    }
    /** Таймлайн анимации высоты (внутреннее использование). */
    protected Timeline heightTimeline;
    /** Таймлайн анимации прозрачности кнопки (внутреннее использование). */
    protected Timeline mainButtonOpacityTimeline;
    /**
     * Анимация прозрачности кнопки (линейная, указанная длительность).
     * <p>
     * Останавливает предыдущую анимацию opacity и плавно переходит от from → to.
     * </p>
     *
     * @param button целевой узел
     * @param fromOpacity начальная прозрачность
     * @param toOpacity конечная прозрачность
     * @param durationMillis длительность в мс
     */
    public void animateButtonOpacity(Node button, double fromOpacity, double toOpacity, int durationMillis) {
        if(mainButtonOpacityTimeline != null) {
            mainButtonOpacityTimeline.stop();
        }

        mainButtonOpacityTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(button.opacityProperty(), fromOpacity)),
                new KeyFrame(Duration.millis(durationMillis), new KeyValue(button.opacityProperty(), toOpacity))
        );

        mainButtonOpacityTimeline.play();
    }
    /**
     * Анимация высоты панели (400мс, back-ease-out SPLINE).
     * <p>
     * Останавливает предыдущую анимацию и плавно изменяет {@link #prefHeightProperty()}
     * до целевой высоты. Выполняет коллбэк <i>до</i> запуска анимации.
     * </p>
     *
     * @param targetHeight целевая высота
     * @param event коллбэк для синхронизации (например, анимация кнопки)
     */
    protected void animateHeight(double targetHeight, Runnable event) {
        if(heightTimeline != null) {
            heightTimeline.stop();
        }

        heightTimeline = new Timeline(
                new KeyFrame(Duration.millis(400),
                        new KeyValue(prefHeightProperty(), targetHeight, Interpolator.SPLINE(0.075, 0.82, 0.165, 1))
                )
        );

        if(event != null) {
            event.run();
        }

        heightTimeline.play();
    }
}
