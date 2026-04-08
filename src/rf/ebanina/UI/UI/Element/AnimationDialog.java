package rf.ebanina.UI.UI.Element;

import javafx.animation.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.CacheHint;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import rf.ebanina.UI.Root;

/**
 * Анимированный модальный диалог с расширенными визуальными эффектами и коллбэками жизненного цикла.
 * <p>
 * Класс расширяет базовый {@code Dialog}, добавляя сложные анимации показа/скрытия,
 * динамическую верхнюю рамку с тенью и события {@link #onShowProperty()} / {@link #onHideProperty()}
 * для интеграции с бизнес-логикой приложения.
 * </p>
 *
 * <h3>Визуальные эффекты</h3>
 * <ul>
 *   <li><b>Показ</b>: одновременная анимация затухания затемнения, подъёма снизу, масштабирования и прозрачности диалога.</li>
 *   <li><b>Скрытие</b>: плавное уменьшение, смещение вниз и исчезновение с полным сбросом состояния.</li>
 *   <li><b>Динамическая рамка</b>: анимированная тень и цвет верхней границы через {@link #animationTopBorder(Color)}.</li>
 * </ul>
 *
 * <h3>Сценарии использования</h3>
 * <ul>
 *   <li>Модальные окна редактирования/настроек с премиум-анимациями.</li>
 *   <li>Диалоги подтверждения с кастомными цветами брендинга.</li>
 *   <li>Информационные попапы, требующие плавного появления/исчезновения.</li>
 * </ul>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * AnimationDialog dialog = new AnimationDialog(primaryStage, rootPane);
 * dialog.setOnShow(e -> loadDataForDialog());
 * dialog.setOnHide(e -> saveDialogData());
 * dialog.setTopBorder(Color.web("#4A90E2"));
 * dialog.show();
 * }</pre>
 *
 * Диалог автоматически привязывается к размеру родительского контейнера и полностью
 * управляет своим жизненным циклом через переопределённые {@link #show()} и {@link #hide()}.
 *
 * @author Ebanina Std
 * @since 0.1.4.4
 * @see Dialog
 * @see ParallelTransition
 * @see DropShadow
 */
public class AnimationDialog
        extends Dialog
{
    private final TranslateTransition slideInTransition;
    private final TranslateTransition slideOutTransition;
    private final FadeTransition fadeInBox;
    private final FadeTransition fadeOutBox;
    private final FadeTransition fadeInDim;
    private final FadeTransition fadeOutDim;
    private final ParallelTransition showTransition;
    private final ParallelTransition hideTransition;

    private final Interpolator bounceInterpolator = new Interpolator() {
        @Override
        protected double curve(double t) {
            return (t == 0) ? 0 : Math.pow(2.0, 10.0 * (t - 1.0));
        }
    };

    /**
     * Активная анимация изменения радиуса тени верхней рамки.
     */
    private Timeline activeBorderAnim;
    /**
     * Текущий эффект тени, применённый к dialogBox.
     */
    private DropShadow activeShadow;
    /**
     * Обработчик события показа диалога.
     * <p>
     * Вызывается после завершения полной анимации появления. Полезно для асинхронной
     * загрузки данных или фокусировки на элементах управления.
     * </p>
     */
    private final ObjectProperty<EventHandler<Event>> onShow = new SimpleObjectProperty<>();
    /**
     * Обработчик события скрытия диалога.
     * <p>
     * Вызывается после завершения анимации скрытия и сброса состояния. Идеально
     * подходит для сохранения данных или разблокировки UI.
     * </p>
     */
    private final ObjectProperty<EventHandler<Event>> onHide = new SimpleObjectProperty<>();
    /**
     * Создаёт диалог, автоматически привязанный к размеру родительского контейнера.
     * <p>
     * Диалог добавляется в {@code root.getChildren()} и синхронизирует размеры
     * через двустороннее связывание {@code prefWidth/HeightProperty}.
     * </p>
     *
     * @param ownerStage родительское окно
     * @param root родительский контейнер (обычно {@link Pane}), определяющий размер диалога
     */
    public AnimationDialog(Stage ownerStage, Pane root) {
        super(ownerStage);

        root.getChildren().add(this);

        prefHeightProperty().bind(root.heightProperty());
        prefWidthProperty().bind(root.widthProperty());

        slideInTransition = new TranslateTransition(Duration.millis(650), dialogBox);
        slideInTransition.setToY(0);
        slideInTransition.setInterpolator(Root.iceInterpolator);

        fadeInBox = new FadeTransition(Duration.millis(200), dialogBox);
        fadeInBox.setToValue(1.0);

        fadeInDim = new FadeTransition(Duration.millis(350), backgroundDim);
        fadeInDim.setToValue(1.0);

        showTransition = new ParallelTransition(slideInTransition, fadeInBox, fadeInDim);

        slideOutTransition = new TranslateTransition(Duration.millis(450), dialogBox);
        slideOutTransition.setInterpolator(bounceInterpolator);

        fadeOutBox = new FadeTransition(Duration.millis(250), dialogBox);
        fadeOutBox.setDelay(Duration.millis(150));
        fadeOutBox.setToValue(0);

        fadeOutDim = new FadeTransition(Duration.millis(400), backgroundDim);
        fadeOutDim.setToValue(0);

        hideTransition = new ParallelTransition(slideOutTransition, fadeOutBox, fadeOutDim);

        hideTransition.setOnFinished(e -> {
            this.setVisible(false);
            dialogBox.setTranslateY(0);
            dialogBox.setOpacity(1.0);
            if (getOnHide() != null)
                getOnHide().handle(new Event(Event.ANY));
            if (onAction != null)
                onAction.run();
        });
    }
    /**
     * Устанавливает статическую верхнюю рамку с тенью заданного цвета.
     * <p>
     * Применяет {@link Border} только к верхней границе с радиусом скругления 14px
     * и создаёт статическую {@link DropShadow} с радиусом 25px.
     * </p>
     *
     * @param color цвет рамки и тени
     */
    public void setTopBorder(Color color) {
        dialogBox.setBorder(new Border(
                new BorderStroke(color,
                        BorderStrokeStyle.SOLID,
                        new CornerRadii(14),
                        new BorderWidths(1, 0, 0, 0)
                )
        ));

        DropShadow shadow = new DropShadow();
        shadow.setRadius(25);
        shadow.setColor(color);
        shadow.setSpread(0.2);
        shadow.setBlurType(BlurType.GAUSSIAN);

        dialogBox.setEffect(shadow);
    }
    /**
     * Запускает анимацию изменения цвета и размера тени верхней рамки.
     * <p>
     * Останавливает предыдущую анимацию, обновляет цвет активной тени и запускает
     * плавное изменение радиуса от текущего значения до 25px за 800мс.
     * </p>
     *
     * @param color целевой цвет тени
     * @return запущенная {@link Timeline} для возможного управления (остановка, пауза)
     */
    public Timeline animationTopBorder(Color color) {
        if (activeBorderAnim != null) {
            activeBorderAnim.stop();
        }

        if (activeShadow == null) {
            activeShadow = new DropShadow();
            activeShadow.setBlurType(BlurType.GAUSSIAN);
            dialogBox.setEffect(activeShadow);
        }

        activeShadow.setColor(color);
        activeShadow.setSpread(0.35);

        activeBorderAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(activeShadow.radiusProperty(), activeShadow.getRadius())),
                new KeyFrame(Duration.millis(800), new KeyValue(activeShadow.radiusProperty(), 25))
        );

        return activeBorderAnim;
    }
    /**
     * Устанавливает обработчик события показа диалога.
     *
     * @param value новый обработчик
     */
    public final void setOnShow(EventHandler<Event> value) {
        onShow.set(value);

    }
    /**
     * Возвращает текущий обработчик события показа.
     *
     * @return обработчик или {@code null}
     */
    public final EventHandler<Event> getOnShow() {
        return onShow.get();
    }
    /**
     * Свойство обработчика события показа.
     *
     * @return свойство для биндинга
     */
    public final ObjectProperty<EventHandler<Event>> onShowProperty() {
        return onShow;
    }
    /**
     * Устанавливает обработчик события скрытия диалога.
     *
     * @param value новый обработчик
     */
    public final void setOnHide(EventHandler<Event> value) {
        onHide.set(value);
    }
    /**
     * Возвращает текущий обработчик события скрытия.
     *
     * @return обработчик или {@code null}
     */
    public final EventHandler<Event> getOnHide() {
        return onHide.get();
    }
    /**
     * Свойство обработчика события скрытия.
     *
     * @return свойство для биндинга
     */
    public final ObjectProperty<EventHandler<Event>> onHideProperty() {
        return onHide;
    }
    /**
     * Анимация показа диалога (параллельная композиция переходов).
     */
    private ParallelTransition showAnimation;
    /**
     * Возвращает объект анимации показа для ручного управления.
     *
     * @return последняя созданная анимация показа
     */
    public ParallelTransition getShowAnimation() {
        return showAnimation;
    }
    /**
     * Переопределённый метод показа с упрощённой анимацией скольжения.
     * <p>
     * Выполняет параллельную анимацию:
     * <ul>
     *   <li>Скольжение снизу вверх (700→0px, 650мс, {@link Root#iceInterpolator}).</li>
     *   <li>Появление диалога (200мс).</li>
     *   <li>Затемнение фона (350мс).</li>
     * </ul>
     * </p>
     */
    @Override
    public void show() {
        dialogBox.setCache(true);
        dialogBox.setCacheHint(CacheHint.QUALITY);

        dialogBox.setTranslateY(700);
        dialogBox.setOpacity(0);
        this.setVisible(true);

        showTransition.setOnFinished(e -> dialogBox.setCache(false));

        showTransition.stop();
        showTransition.play();
    }
    /**
     * Переопределённый метод скрытия с bounce-эффектом.
     * <p>
     * Выполняет параллельную анимацию:
     * <ul>
     *   <li>Скольжение вниз (0→800px, 450мс, кастомный экспоненциальный bounce).</li>
     *   <li>Задержанное исчезновение диалога (250мс + 150мс delay).</li>
     *   <li>Исчезновение затемнения (400мс).</li>
     * </ul>
     * После завершения вызывает {@link #getOnHide()} и {@code onAction}.
     * </p>
     */
    @Override
    public void hide() {
        if (activeBorderAnim != null) {
            activeBorderAnim.stop();
            activeBorderAnim = null;
        }

        double dialogHeight = dialogBox.localToScene(dialogBox.getBoundsInLocal()).getHeight();
        double hideOffset = Math.max(800, dialogHeight + 200);

        slideOutTransition.setToY(hideOffset);
        hideTransition.stop();
        hideTransition.play();

        dialogBox.setCache(false);
    }
}
