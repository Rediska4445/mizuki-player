package rf.ebanina.UI.UI.Element.ListViews;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.AnimatedListCell;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import static rf.ebanina.utils.Math.clamp;

/**
 * Расширенная версия {@link javafx.scene.control.ListView} с поддержкой плавной прокрутки,
 * улучшенной подсветки скроллбаров и настраиваемого цвета выбора элементов.
 * <p>
 * Класс инкапсулирует типичные визуальные улучшения стандартного списка:
 * анимацию прокрутки, плавное появление/затухание полос прокрутки и единый контроль
 * за цветом выделения через свойство {@link #selectedColorProperty}. Это позволяет
 * использовать компонент как "готовый" виджет списка в тематике приложения без
 * дублирования однотипного кода в разных местах.
 * </p>
 *
 * <h3>Основные возможности</h3>
 * <ul>
 *   <li>Плавная прокрутка при использовании колёсика мыши (анимация через {@link Timeline}).</li>
 *   <li>Анимированная подсветка полос прокрутки при наведении и нажатии (на базе {@link FadeTransition}).</li>
 *   <li>Поддержка кастомного цвета выделения элементов списка через {@link #selectedColorProperty} и {@link AnimatedListCell}.</li>
 *   <li>Централизованное применение CSS-оформления скроллбаров через {@link ResourceManager}.</li>
 * </ul>
 *
 * <h3>Сценарии использования</h3>
 * <ul>
 *   <li>Списки треков, плейлистов, коллекций, где важен плавный и приятный UX.</li>
 *   <li>Интерфейсы с тёмной/кастомной темой, требующие точного контроля над цветами и стилем скроллбаров.</li>
 *   <li>Списки с анимированными ячейками, использующими {@link AnimatedListCell} как cellFactory.</li>
 * </ul>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * ObservableList<MyItem> items = FXCollections.observableArrayList(...);
 * ListView<MyItem> listView = new ListView<>(items);
 *
 * listView.setCellFactory(lv -> new AnimatedListCell<>(listView.getSelectedColorProperty()));
 * listView.updateBorderColor(Color.GRAY);
 * listView.updateSelectedBackground(Color.web("#FF6600"));
 * }</pre>
 *
 * Класс не меняет поведение выбора элементов и работы модели данных стандартного {@code ListView},
 * а лишь добавляет визуальные и анимационные улучшения поверх существующего API.
 *
 * @param <T> тип элементов, отображаемых в списке
 * @author
 *      Ebanina Std
 * @since 0.1.4.4
 * @see javafx.scene.control.ListView
 * @see AnimatedListCell
 * @see ScrollBar
 * @see Timeline
 */
public class ListView<T>
        extends javafx.scene.control.ListView<T>
{
    /**
     * Свойство, определяющее цвет выделения элементов списка.
     * <p>
     * Используется для стилизации границ и фона выбранных {@link ListCell} в соответствии
     * с общей цветовой схемой приложения.
     * </p>
     */
    private ObjectProperty<Color> selectedColorProperty = new SimpleObjectProperty<>(ColorProcessor.core.getMainClr());
    /**
     * Возвращает свойство цвета выделения.
     *
     * @return свойство с текущим цветом выделения
     */
    public ObjectProperty<Color> getSelectedColorProperty() {
        return selectedColorProperty;
    }
    /**
     * Свойство цвета выделения для биндинга.
     *
     * @return свойство цвета выделения
     */
    public ObjectProperty<Color> selectedColorPropertyProperty() {
        return selectedColorProperty;
    }
    /**
     * Заменяет объект свойства цвета выделения.
     *
     * @param selectedColorProperty новое свойство цвета выделения
     */
    public void setSelectedColorProperty(ObjectProperty<Color> selectedColorProperty) {
        this.selectedColorProperty = selectedColorProperty;
    }
    /**
     * Устанавливает цвет выделения.
     *
     * @param selectedColor новый цвет выделения для ячеек списка
     */
    public void setSelectedColor(Color selectedColor) {
        selectedColorProperty.set(selectedColor);
    }
    /**
     * Возвращает текущий цвет выделения.
     *
     * @return активный цвет выделения
     */
    public Color getSelectedColor() {
        return selectedColorProperty.get();
    }
    /**
     * Обновляет цвет рамки списка.
     * <p>
     * Применяет inline-стиль с заданным цветом границы, радиусом скругления и шириной.
     * Полезно для динамического изменения обводки при смене темы или состояния.
     * </p>
     *
     * @param color цвет рамки, который будет использован в стиле
     */
    public void updateBorderColor(Color color) {
        setStyle(null);
        setStyle(
                "-fx-border-color: " + ColorProcessor.core.toHex(color) + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-style: solid;" +
                        "-fx-border-radius: 4;" +
                        "-fx-border-insets: 0;"
        );
    }

    /**
     * Обновляет цвет фона и границ выбранных ячеек согласно новому цвету выделения.
     * <p>
     * Метод проходит по всем {@code .list-cell} и для выделенных элементов устанавливает
     * рамку, создаваемую фабрикой {@link AnimatedListCell#borderFactory(Color)}.
     * </p>
     *
     * @param newColor новый цвет для оформления выделения
     */
    public void updateSelectedBackground(Color newColor) {
        setSelectedColor(newColor);

        for (Node node : lookupAll(".list-cell")) {
            if (node instanceof ListCell<?> cell) {
                if (cell.isSelected()) {
                    Border newBorder = AnimatedListCell.borderFactory(newColor);

                    cell.setBorder(newBorder);
                } else {
                    cell.setBorder(null);
                }
            }
        }
    }
    /**
     * Создаёт пустой список с включёнными анимациями прокрутки и подсветкой скроллбаров.
     */
    public ListView() {
        this(FXCollections.observableArrayList());
    }
    /**
     * Создаёт список с заданными начальными элементами и включает анимации прокрутки
     * и подсветку скроллбаров.
     *
     * @param tracks начальная коллекция элементов списка
     */
    public ListView(ObservableList<T> tracks) {
        super(tracks);

        prepareToScrollAnimation();

        addSmoothScroll();
        addSmoothHighlightOnScrollBars();

        selectedColorProperty.addListener((t, e1, e2) -> updateSelectedBackground(e2));

        setSkin(new ListViewSkin<>(this));
    }
    /**
     * Вертикальный скроллбар списка, используемый для организации анимированной прокрутки.
     */
    private ScrollBar vScrollBar;
    /**
     * Таймлайн, управляющий анимацией плавной прокрутки.
     */
    private Timeline timeline;
    /**
     * Коэффициент чувствительности к колесику мыши при прокрутке.
     */
    private double sensitivity = 0.25;

    /**
     * Целевое значение позиции скролла, к которому стремится анимация.
     */
    private double targetValue = 0;

    /**
     * Флаг, показывающий, перетаскивает ли пользователь ползунок скроллбара вручную.
     */
    private boolean userIsDragging = false;

    /**
     * Включает обработчик плавной прокрутки колёсиком мыши.
     */
    public void addSmoothScroll() {
        addEventFilter(ScrollEvent.SCROLL, smoothScrollEventHandler);
    }

    /**
     * Отключает обработчик плавной прокрутки колёсиком мыши.
     */
    public void removeSmoothScroll() {
        removeEventFilter(ScrollEvent.SCROLL, smoothScrollEventHandler);
    }

    /**
     * Инициализирует поддержку анимированной прокрутки, находя вертикальный скроллбар
     * и подготавливая его к управлению через анимацию.
     * <p>
     * Вызов выполняется отложенно через {@link Platform#runLater(Runnable)}, так как
     * скроллбар доступен только после построения сцены.
     * </p>
     */
    private void prepareToScrollAnimation() {
        Platform.runLater(() -> {
            vScrollBar = findScrollBar(this, Orientation.VERTICAL);

            if (vScrollBar == null) {
                System.err.println("Vertical ScrollBar not found!");
                return;
            }

            targetValue = vScrollBar.getValue();

            makeScrollBarAlwaysVisibleAndStyled(vScrollBar);

            vScrollBar.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                userIsDragging = true;
                if (timeline != null) {
                    timeline.stop();
                }
            });

            vScrollBar.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
                userIsDragging = false;
                targetValue = vScrollBar.getValue();
            });

            vScrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (userIsDragging) {
                    targetValue = newVal.doubleValue();
                }
            });
        });
    }

    /**
     * Включает оформление и анимацию подсветки для вертикального и горизонтального скроллбаров.
     * <p>
     * Загружает фиксированный стиль скроллбаров через {@link ResourceManager}, делает
     * горизонтальный скроллбар всегда видимым и настраивает анимацию появления/исчезновения
     * для обоих скроллбаров.
     * </p>
     */
    public void addSmoothHighlightOnScrollBars() {
        Platform.runLater(() -> {
            getStylesheets().add(ResourceManager.getInstance().loadStylesheet("scrollbar-fixed-width"));

            for (Node node : lookupAll(".scroll-bar")) {
                if (node instanceof ScrollBar sb && sb.getOrientation() == Orientation.HORIZONTAL) {
                    sb.setVisible(true);
                    sb.setManaged(true);
                    sb.setOpacity(1);

                    if (sb.getMax() == 0.0)
                        sb.setMax(1.0);
                }
            }

            ScrollPane scrollPane = (ScrollPane) lookup(".scroll-pane");

            if (scrollPane != null) {
                scrollPane.setVisible(true);
                scrollPane.setManaged(true);
                scrollPane.setOpacity(1.0);
                scrollPane.setPrefHeight(15);
                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            }

            ScrollBar vScrollBar = findScrollBar(this, Orientation.VERTICAL);
            ScrollBar hScrollBar = findScrollBar(this, Orientation.HORIZONTAL);

            if (vScrollBar != null) {
                setupFadeAndPressTransition(vScrollBar);
            }
            if (hScrollBar != null) {
                setupFadeAndPressTransition(hScrollBar);
            }
        });
    }

    /**
     * Настраивает анимации плавного появления, исчезновения и реакции на нажатие для заданного скроллбара.
     *
     * @param scrollBar скроллбар, для которого настраиваются анимации
     */
    private void setupFadeAndPressTransition(ScrollBar scrollBar) {
        scrollBar.setOpacity(0.3);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), scrollBar);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), scrollBar);
        fadeOut.setToValue(0.3);

        FadeTransition fadePress = new FadeTransition(Duration.millis(100), scrollBar);
        fadePress.setToValue(1.0);

        scrollBar.setOnMouseEntered(e -> {
            fadeOut.stop();
            fadeIn.playFromStart();
        });

        scrollBar.setOnMouseExited(e -> {
            fadeIn.stop();
            fadeOut.playFromStart();
        });

        scrollBar.setOnMousePressed(e -> {
            fadeOut.stop();
            fadeIn.stop();
            fadePress.playFromStart();
        });

        scrollBar.setOnMouseReleased(e -> {
            fadePress.stop();
            fadeOut.playFromStart();
        });
    }

    /**
     * Обработчик событий прокрутки, реализующий плавное смещение содержимого списка
     * с учётом количества элементов и выбранной чувствительности.
     */
    private EventHandler<? super ScrollEvent> smoothScrollEventHandler = new EventHandler<>() {
        @Override
        public void handle(ScrollEvent event) {
            event.consume();

            if (userIsDragging) {
                return;
            }

            int cellCount = getItems().size();
            double delta = event.getDeltaY() * sensitivity * (1.0 / java.lang.Math.max(1, cellCount));

            if(vScrollBar == null)
                return;

            targetValue = clamp(targetValue - delta, vScrollBar.getMin(), vScrollBar.getMax());

            if (timeline == null || timeline.getStatus() != Animation.Status.RUNNING) {
                timeline = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(vScrollBar.valueProperty(), vScrollBar.getValue())),
                        new KeyFrame(Duration.millis(600), new KeyValue(vScrollBar.valueProperty(), targetValue, Interpolator.SPLINE(0.25, 0.1, 0.25, 1)))
                );

                timeline.play();
            } else {
                KeyValue kv = new KeyValue(vScrollBar.valueProperty(), targetValue, Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
                KeyFrame kf = new KeyFrame(Duration.millis(600), kv);

                timeline.stop();
                timeline.getKeyFrames().setAll(
                        new KeyFrame(Duration.ZERO, new KeyValue(vScrollBar.valueProperty(), vScrollBar.getValue())),
                        kf
                );

                timeline.playFromStart();
            }
        }
    };

    /**
     * Ищет скроллбар заданной ориентации внутри указанного {@link javafx.scene.control.ListView}.
     *
     * @param listView    список, внутри которого производится поиск
     * @param orientation требуемая ориентация скроллбара (вертикальная или горизонтальная)
     * @return найденный {@link ScrollBar} или {@code null}, если подходящий экземпляр не обнаружен
     */
    private ScrollBar findScrollBar(javafx.scene.control.ListView<?> listView, Orientation orientation) {
        for (var node : listView.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar sb && sb.getOrientation() == orientation) {
                return sb;
            }
        }

        return null;
    }

    /**
     * Делает указанный скроллбар всегда видимым и применяет к нему базовый стиль.
     * <p>
     * Настраивает прозрачность, фон и отступы, чтобы скроллбар аккуратно вписывался
     * в общий дизайн приложения.
     * </p>
     *
     * @param scrollBar скроллбар, к которому применяется оформление
     */
    private void makeScrollBarAlwaysVisibleAndStyled(ScrollBar scrollBar) {
        scrollBar.setVisible(true);
        scrollBar.setOpacity(0.5);
        scrollBar.setMouseTransparent(false);
        scrollBar.setPadding(new Insets(0));
        scrollBar.setBackground(new Background(new BackgroundFill(
                Color.TRANSPARENT,
                new CornerRadii(5),
                new Insets(0,0,0,0))
        ));
    }
}
