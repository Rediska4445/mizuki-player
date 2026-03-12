package rf.ebanina.UI.UI.Element;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * <h1>Dialog</h1>
 * Абстрактный базовый класс для создания современных модальных диалоговых окон с анимациями
 * и расширенной кастомизацией в JavaFX-приложении.
 * <p>
 * Класс предоставляет готовую инфраструктуру для создания диалогов с:
 * <ul>
 *   <li><b>Анимированное появление/исчезновение</b> — плавный scale + fade эффекты.</li>
 *   <li><b>Полностью настраиваемый фон затемнения</b> — цвет, прозрачность, кликабельность.</li>
 *   <li><b>Централизованное управление содержимым</b> через {@link #getDialogBox()} (VBox).</li>
 *   <li><b>Автоматическое масштабирование</b> под размер родительского Stage.</li>
 *   <li><b>Поддержка модальности</b> — блокировка взаимодействий с родительским окном.</li>
 * </ul>
 * </p>
 * <p>
 * Диалог представляет собой {@link StackPane} с двумя детьми:
 * <ol>
 *   <li><b>backgroundDim</b> — затемнённый фон, привязанный к размерам {@link #ownerStage}.</li>
 *   <li><b>dialogBox</b> — VBox-контейнер для пользовательского контента с отступами 25px.</li>
 * </ol>
 * </p>
 * <p>
 * Класс является <code>abstract</code> — наследники должны добавлять свой контент в
 * {@link #getDialogBox()} и могут переопределять методы {@link #show()} и {@link #hide()}.
 * </p>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * // Создание диалога
 * AnimationDialog dialog = new AnimationDialog(ownerStage, rootPane);
 * dialog.setDialogMaxSize(0.7, 0.85);
 * dialog.setTopBorder(ColorProcessor.core.getMainClr());
 *
 * // Добавление контента
 * VBox content = dialog.getDialogBox();
 * content.getChildren().addAll(label, button, listView);
 *
 * // Показ диалога
 * dialog.initModality(Modality.APPLICATION_MODAL);
 * dialog.show();
 * }</pre>
 *
 * <h3>Жизненный цикл</h3>
 * <ul>
 *   <li><b>Создание</b>: диалог невидим ({@link #setVisible(boolean)}}).</li>
 *   <li><b>{@link #show()}</b>: анимация появления (450мс) → видимый.</li>
 *   <li><b>{@link #hide()}</b>: анимация исчезновения (350мс) → невидимый + {@link #onAction}.</li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 0.1.4.8
 * @see AnimationDialog
 * @see javafx.stage.Modality
 * @see VBox
 */
public abstract class Dialog
        extends StackPane
{
    /**
     * Затемнённый фон диалога, автоматически привязанный к размерам {@link #ownerStage}.
     * <p>
     * По умолчанию: <code>rgba(0, 0, 0, 0.65)</code> с opacity=0. Клик по фону вызывает {@link #hide()}.
     * </p>
     * <p>
     * Управление через методы:
     * <ul>
     *   <li>{@link #setBackgroundOpacity(double)} — прозрачность.</li>
     *   <li>{@link #setBackgroundColor(Color, double)} — цвет.</li>
     *   <li>{@link #setCloseOnBackgroundClick(boolean)} — кликабельность.</li>
     * </ul>
     * </p>
     */
    protected final Pane backgroundDim;
    /**
     * Контейнер пользовательского контента (VBox с spacing=20).
     * <p>
     * Характеристики по умолчанию:
     * <ul>
     *   <li>Фон: <code>#1E1E1E</code> с радиусом 16px.</li>
     *   <li>Граница: <code>Color.gray(0.3)</code> толщиной 1px.</li>
     *   <li>Отступы: <code>Insets(25)</code>.</li>
     *   <li>Выравнивание: <code>Pos.CENTER</code>.</li>
     * </ul>
     * Используйте {@link #getDialogBox()} для добавления элементов.
     * </p>
     */
    protected final VBox dialogBox;
    /**
     * Обработчик, выполняемый после завершения анимации {@link #hide()}.
     * <p>
     * Может быть установлен через {@link #setOnAction(Runnable)}. По умолчанию <code>null</code>.
     * Вызывается <b>после</b> установки {@link #setVisible(boolean)}}.
     * </p>
     */
    protected Runnable onAction;
    /**
     * Родительский Stage, к которому привязаны размеры фона.
     * <p>
     * Устанавливается в конструкторе и используется для:
     * <ul>
     *   <li>Автоматического масштабирования {@link #backgroundDim}.</li>
     *   <li>Вычисления размеров {@link #dialogBox} в {@link #setDialogMaxSize(double, double)}.</li>
     * </ul>
     * </p>
     */
    protected final Stage ownerStage;
    /**
     * Создаёт новый диалог, привязанный к указанному родительскому окну.
     * <p>
     * Инициализирует:
     * <ul>
     *   <li>{@link #backgroundDim} с затемнением и обработчиком клика.</li>
     *   <li>{@link #dialogBox} с тёмным фоном, границей и отступами.</li>
     *   <li>Привязку фона к размерам {@link #ownerStage} через {@link #bindBackgroundToStage()}.</li>
     *   <li>Невидимость диалога ({@link #setVisible(boolean)}}).</li>
     * </ul>
     * </p>
     * <p>
     * После создания диалог готов к добавлению контента через {@link #getDialogBox()}.
     * </p>
     *
     * @param ownerStage родительское окно; не должно быть <code>null</code>
     * @throws NullPointerException если <code>ownerStage</code> равен <code>null</code>
     */
    public Dialog(Stage ownerStage) {
        this.ownerStage = ownerStage;

        backgroundDim = new Pane();
        backgroundDim.setStyle("-fx-background-color: rgba(0, 0, 0, 0.65);");
        backgroundDim.setOpacity(0);
        backgroundDim.setOnMouseClicked(this::hideOnBackgroundClick);

        dialogBox = new VBox(20);
        dialogBox.setAlignment(Pos.CENTER);
        dialogBox.setPadding(new Insets(25));
        dialogBox.setBackground(new Background(new BackgroundFill(
                Color.web("#1E1E1E"), new CornerRadii(16), Insets.EMPTY
        )));
        dialogBox.setOpacity(0);

        dialogBox.setBorder(new Border(new BorderStroke(
                Color.gray(0.3), BorderStrokeStyle.SOLID, new CornerRadii(16), new BorderWidths(1)
        )));

        bindBackgroundToStage();
        this.getChildren().addAll(backgroundDim, dialogBox);
        this.setVisible(false);
    }
    /**
     * Привязывает размеры фона к родительскому Stage.
     * <p>
     * Устанавливает <b>двунаправленные привязки</b> для <code>pref</code>, <code>min</code> и <code>max</code>
     * размеров {@link #backgroundDim} к соответствующим свойствам {@link #ownerStage}.
     * </p>
     * <p>
     * Вызывается автоматически в конструкторе. Предназначен только для внутреннего использования.
     * </p>
     */
    private void bindBackgroundToStage() {
        backgroundDim.prefWidthProperty().bind(ownerStage.widthProperty());
        backgroundDim.prefHeightProperty().bind(ownerStage.heightProperty());
        backgroundDim.minWidthProperty().bind(ownerStage.widthProperty());
        backgroundDim.minHeightProperty().bind(ownerStage.heightProperty());
        backgroundDim.maxWidthProperty().bind(ownerStage.widthProperty());
        backgroundDim.maxHeightProperty().bind(ownerStage.heightProperty());
    }
    /**
     * Устанавливает максимальные размеры контейнера контента как коэффициенты от родительского Stage.
     * <p>
     * Размеры вычисляются как <code>ownerStage.getWidth() * widthFactor</code>.
     * </p>
     * <p>
     * <b>Примечание</b>: использует <i>текущие</i> размеры Stage. Для динамического изменения
     * рекомендуется вызывать перед {@link #show()} или использовать привязки.
     * </p>
     *
     * @param widthFactor коэффициент ширины (0.0-1.0)
     * @param heightFactor коэффициент высоты (0.0-1.0)
     * @see #setDialogSize(double, double)
     */
    public void setDialogMaxSize(double widthFactor, double heightFactor) {
        dialogBox.setMaxSize(
                ownerStage.getWidth() * widthFactor,
                ownerStage.getHeight() * heightFactor
        );
    }
    /**
     * Устанавливает фиксированные размеры контейнера контента.
     * <p>
     * Устанавливает одинаковые значения для <code>prefSize</code> и <code>maxSize</code>,
     * что делает контейнер заданного размера без растягивания.
     * </p>
     *
     * @param width фиксированная ширина
     * @param height фиксированная высота
     * @see #setDialogMaxSize(double, double)
     */
    public void setDialogSize(double width, double height) {
        dialogBox.setPrefSize(width, height);
        dialogBox.setMaxSize(width, height);
    }
    /**
     * Устанавливает внутренние отступы контейнера контента.
     * <p>
     * Заменяет текущие отступы {@link #dialogBox}. По умолчанию <code>Insets(25)</code>.
     * </p>
     *
     * @param padding новые отступы
     */
    public void setDialogPadding(Insets padding) {
        dialogBox.setPadding(padding);
    }
    /**
     * Устанавливает уровень затемнения фона (0.0 = прозрачный, 1.0 = полностью чёрный).
     * <p>
     * Ограничивает значение диапазоном [0.0, 1.0] и применяет CSS-стиль <code>rgba(0,0,0,opacity)</code>.
     * Не влияет на текущую непрозрачность {@link #backgroundDim#setOpacity(double)}}, которая управляется анимацией.
     * </p>
     *
     * @param opacity уровень затемнения (0.0-1.0)
     */
    public void setBackgroundOpacity(double opacity) {
        opacity = Math.max(0.0, Math.min(1.0, opacity));
        String rgba = String.format("rgba(0, 0, 0, %.2f)", opacity);
        backgroundDim.setStyle("-fx-background-color: " + rgba + ";");
    }
    /**
     * Устанавливает цвет и прозрачность фона затемнения.
     * <p>
     * Форматирует цвет в <code>rgba(r,g,b,opacity)</code> и применяет как CSS-стиль.
     * Ограничивает прозрачность диапазоном [0.0, 1.0].
     * </p>
     *
     * @param color цвет фона
     * @param opacity прозрачность (0.0-1.0)
     */
    public void setBackgroundColor(Color color, double opacity) {
        opacity = Math.max(0.0, Math.min(1.0, opacity));
        String rgba = String.format("rgba(%.0f, %.0f, %.0f, %.2f)",
                color.getRed() * 255,
                color.getGreen() * 255,
                color.getBlue() * 255,
                opacity);
        backgroundDim.setStyle("-fx-background-color: " + rgba + ";");
    }
    /**
     * Включает или выключает закрытие диалога по клику на фон.
     * <p>
     * При <code>enabled=true</code> регистрирует обработчик {@link #hideOnBackgroundClick(MouseEvent)}.
     * При <code>enabled=false</code> удаляет обработчик.
     * </p>
     *
     * @param enabled true = включить, false = выключить
     */
    public void setCloseOnBackgroundClick(boolean enabled) {
        if (enabled) {
            backgroundDim.setOnMouseClicked(this::hideOnBackgroundClick);
        } else {
            backgroundDim.setOnMouseClicked(null);
        }
    }
    /**
     * Устанавливает цвет фона контейнера контента.
     * <p>
     * Создаёт новый <code>BackgroundFill</code> с фиксированным радиусом углов 16px.
     * </p>
     *
     * @param color новый цвет фона
     */
    public void setDialogBackgroundColor(Color color) {
        dialogBox.setBackground(new Background(new BackgroundFill(
                color, new CornerRadii(16), Insets.EMPTY
        )));
    }
    /**
     * Устанавливает радиус скругления углов контейнера контента.
     * <p>
     * Пересоздаёт <code>Background</code> и <code>Border</code> с новым радиусом.
     * Фон остаётся <code>#1E1E1E</code>, граница — <code>Color.gray(0.3)</code>.
     * </p>
     *
     * @param radius новый радиус скругления (пиксели)
     */
    public void setDialogCornerRadius(double radius) {
        // Требует пересоздания background и border
        dialogBox.setBackground(new Background(new BackgroundFill(
                Color.web("#1E1E1E"), new CornerRadii(radius), Insets.EMPTY
        )));
        dialogBox.setBorder(new Border(new BorderStroke(
                Color.gray(0.3), BorderStrokeStyle.SOLID, new CornerRadii(radius), new BorderWidths(1)
        )));
    }
    /**
     * Устанавливает расстояние между элементами в контейнере (spacing).
     * <p>
     * Изменяет свойство <code>spacing</code> у {@link #dialogBox}. По умолчанию 20px.
     * </p>
     *
     * @param spacing расстояние между дочерними элементами (пиксели)
     */
    public void setDialogSpacing(double spacing) {
        dialogBox.setSpacing(spacing);
    }
    /**
     * Внутренний обработчик клика по фону.
     * <p>
     * Закрывает диалог только при клике непосредственно по {@link #backgroundDim},
     * игнорируя клики по дочерним элементам.
     * </p>
     *
     * @param event событие мыши
     */
    private void hideOnBackgroundClick(MouseEvent event) {
        if (event.getTarget() == backgroundDim) {
            hide();
        }
    }
    /**
     * Показывает диалог с анимацией появления (450мс).
     * <p>
     * Последовательность:
     * <ol>
     *   <li>{@link #setVisible(boolean)}} — делает диалог видимым.</li>
     *   <li><b>FadeIn backgroundDim</b> (300мс) — затемнение фона.</li>
     *   <li><b>FadeIn dialogBox</b> (350мс) — появление контента.</li>
     *   <li><b>Scale</b> от 0.8→1.0 (350мс, linear) — "пружина" контента.</li>
     * </ol>
     * </p>
     * <p>
     * Наследники могут переопределить для добавления эффектов (тени, сдвиги).
     * </p>
     */
    public void show() {
        this.setVisible(true);

        FadeTransition fadeInDim = new FadeTransition(Duration.millis(300), backgroundDim);
        fadeInDim.setToValue(1.0);

        FadeTransition fadeInBox = new FadeTransition(Duration.millis(350), dialogBox);
        fadeInBox.setToValue(1.0);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(350), dialogBox);
        scaleIn.setFromX(0.8);
        scaleIn.setFromY(0.8);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        scaleIn.setInterpolator(Interpolator.LINEAR);

        new ParallelTransition(fadeInDim, fadeInBox, scaleIn).play();
    }
    /**
     * Скрывает диалог с анимацией исчезновения (350мс).
     * <p>
     * Последовательность:
     * <ol>
     *   <li><b>FadeOut</b> всего диалога (200мс) — исчезновение.</li>
     *   <li><b>Scale</b> dialogBox до 0.9 (200мс) — "сжатие".</li>
     *   <li><b>OnFinished</b>: {@link #setVisible(boolean)}}, сброс opacity, вызов {@link #onAction}.</li>
     * </ol>
     * </p>
     * <p>
     * Наследники могут переопределить для добавления дополнительных эффектов.
     * </p>
     */
    public void hide() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), this);
        fadeOut.setToValue(0);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), dialogBox);
        scaleOut.setToX(0.9);
        scaleOut.setToY(0.9);

        ParallelTransition hideAnim = new ParallelTransition(fadeOut, scaleOut);
        hideAnim.setOnFinished(e -> {
            this.setVisible(false);
            this.setOpacity(1.0);
            if (onAction != null) onAction.run();
        });
        hideAnim.play();
    }
    /**
     * Устанавливает обработчик, выполняемый после завершения {@link #hide()}.
     * <p>
     * Вызывается в <code>OnFinished</code> анимации скрытия, после {@link #setVisible(boolean)}}.
     * Заменяет предыдущий обработчик.
     * </p>
     *
     * @param onAction новый обработчик; <code>null</code> отключает обработчик
     */
    public void setOnAction(Runnable onAction) {
        this.onAction = onAction;
    }
    /**
     * Возвращает контейнер для добавления пользовательского контента.
     * <p>
     * Это <b>единственный способ</b> добавления элементов в диалог:
     * <pre>{@code
     * VBox content = dialog.getDialogBox();
     * content.getChildren().addAll(label, button, listView);
     * VBox.setVgrow(listView, Priority.ALWAYS);
     * }</pre>
     * </p>
     *
     * @return VBox-контейнер контента
     */
    public VBox getDialogBox() {
        return dialogBox;
    }
}
