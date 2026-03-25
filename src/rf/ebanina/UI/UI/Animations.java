package rf.ebanina.UI.UI;

import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.lang.ref.WeakReference;
import java.util.Set;

/**
 * <h1>Animations</h1>
 * Утилитарный класс для централизованного управления анимациями в JavaFX-приложении.
 * <p>
 * Этот класс решает критическую проблему производительности и пользовательского опыта:
 * управление анимациями в зависимости от состояния окна приложения. Он позволяет
 * приложению "умно" вести себя, когда окно не в фокусе или свёрнуто, предотвращая
 * ненужную нагрузку на CPU и GPU.
 * </p>
 * <p>
 * Основные состояния, которые отслеживает класс:
 * <ul>
 *   <li><b>Окно в фокусе</b> — все анимации работают в полную силу.</li>
 *   <li><b>Окно не в фокусе</b> — запускаются только "разрешённые" анимации (например, переходы при закрытии).</li>
 *   <li><b>Окно свёрнуто</b> — все анимации подавляются, чтобы сэкономить ресурсы системы.</li>
 * </ul>
 * </p>
 * <p>
 * Класс является <code>final</code> и не предназначен для наследования. Все методы
 * статические, что делает его удобным для использования в любом месте приложения
 * без необходимости создания экземпляра.
 * </p>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * // Инициализация в главном классе приложения
 * Animations.init(primaryStage);
 *
 * // В другом месте кода
 * FadeTransition fadeOut = new FadeTransition(Duration.millis(300), node);
 * fadeOut.setFromValue(1.0);
 * fadeOut.setToValue(0.0);
 *
 * // Безопасный запуск анимации
 * Animations.play(fadeOut, "outTransition");
 * }</pre>
 *
 * <h3>Особенности</h3>
 * <ul>
 *   <li>Класс не управляет жизненным циклом анимаций (остановка, пауза) — только их запуск.</li>
 *   <li>Состояние "не в фокусе" определяется по свойству {@link Stage#focusedProperty()},
 *       что может не учитывать все сценарии (например, системные уведомления).</li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 0.1.4.4
 * @see Animation
 * @see Stage
 * @see #init(Stage)
 */
public class Animations {
    public static Animations instance = new Animations();

    /**
     * Флаг, указывающий, находится ли приложение в фокусе.
     * <p>
     * Устанавливается в <code>true</code>, когда окно приложения активно и имеет фокус ввода.
     * В противном случае — <code>false</code>. Значение обновляется автоматически
     * через слушатель, зарегистрированный в методе {@link #init(Stage)}.
     * </p>
     * <p>
     * Это состояние определяет, какие анимации могут быть запущены. Например,
     * анимации появления новых элементов могут быть подавлены, чтобы не отвлекать пользователя.
     * </p>
     *
     * @see #init(Stage)
     * @see #play(Animation, String)
     * @since 0.1.4.4
     */
    private volatile boolean isAppFocused = true;
    /**
     * Флаг, указывающий, свёрнуто ли окно приложения.
     * <p>
     * Устанавливается в <code>true</code>, когда пользователь сворачивает окно.
     * В этом состоянии все анимации подавляются, независимо от их типа.
     * Это критически важно для экономии ресурсов системы и предотвращения
     * ненужной работы GPU.
     * </p>
     * <p>
     * Значение обновляется автоматически через слушатель, зарегистрированный
     * в методе {@link #init(Stage)}.
     * </p>
     *
     * @see #init(Stage)
     * @see #play(Animation, String)
     * @since 0.1.4.4
     */
    private volatile boolean isAppMinimized = false;
    /**
     * Множество идентификаторов анимаций, которые разрешены к запуску, когда приложение не в фокусе.
     * <p>
     * Содержит анимации, которые считаются "важными" или "неотвлекающими", даже если
     * пользователь работает с другим приложением. Например:
     * <ul>
     *   <li><code>outTransition</code> — анимация исчезновения элемента.</li>
     *   <li><code>outDropShadowAnimation</code> — анимация исчезновения тени.</li>
     *   <li><code>inTransition</code> — анимация появления элемента (в некоторых случаях).</li>
     *   <li><code>inDropShadowAnimation</code> — анимация появления тени.</li>
     * </ul>
     * </p>
     * <p>
     * Использование {@link java.util.HashSet} обеспечивает быструю проверку наличия
     * идентификатора (O(1)).
     * </p>
     *
     * @see #play(Animation, String)
     * @since 0.1.4.4
     */
    protected final Set<String> allowedAnimationsWhenUnfocused = Set.of("outTransition", "outDropShadowAnimation", "inTransition", "inDropShadowAnimation");

    /**
     * Ссылка на Stage для возможности удаления слушателей
     */
    protected WeakReference<Stage> stageRef;

    /**
     * Поля для хранения ссылок на слушатели (необходимо для их удаления)
     */
    private javafx.beans.value.ChangeListener<Boolean> focusListener;
    private javafx.beans.value.ChangeListener<Boolean> iconifiedListener;

    /**
     * Безопасно запускает анимацию в зависимости от текущего состояния приложения.
     * <p>
     * Метод проверяет состояние окна и принимает решение о запуске анимации:
     * </p>
     * <ol>
     *   <li>
     *     <b>Окно свёрнуто</b>: если {@link #isAppMinimized} равно {@code true},
     *     анимация немедленно останавливается через {@link Animation#stop()} и не запускается.
     *   </li>
     *   <li>
     *     <b>Окно не в фокусе</b>: если {@link #isAppFocused} равно {@code false},
     *     проверяется наличие {@code animationId} в множестве {@link #allowedAnimationsWhenUnfocused}.
     *     Если идентификатор не разрешён — анимация останавливается и не запускается.
     *   </li>
     *   <li>
     *     <b>Окно в фокусе</b>: если анимация ещё не запущена
     *     ({@link Animation#getStatus()} ≠ {@link javafx.animation.Animation.Status#RUNNING}),
     *     она запускается через {@link Animation#play()}. Если уже запущена — метод возвращает
     *     управление без повторного запуска.
     *   </li>
     * </ol>
     *
     * <h3>Пример логики</h3>
     * <pre>{@code
     * if (свёрнуто) → stop(), return animation
     * if (не в фокусе && !разрешено) → stop(), return animation
     * if (уже запущена) → return animation
     * else → play(), return animation
     * }</pre>
     *
     * @param animation   объект анимации для запуска; не должен быть {@code null}
     * @param animationId уникальный идентификатор анимации (например, {@code "fadeIn"},
     *                    {@code "outTransition"}); используется для проверки разрешённых
     *                    анимаций, когда окно не в фокусе
     * @return переданный объект анимации, независимо от того, был ли он запущен
     * @throws NullPointerException если {@code animation} равен {@code null}
     * @see #play(Animation, String, Runnable)
     * @see Animation#play()
     * @see Animation#stop()
     * @since 0.1.4.4
     */
    public Animation play(Animation animation, String animationId) {
        if (animation == null) {
            throw new NullPointerException("animation cannot be null");
        }

        if (isAppMinimized) {
            animation.stop();

            return animation;
        }

        if (!isAppFocused) {
            if (!allowedAnimationsWhenUnfocused.contains(animationId)) {
                animation.stop();

                return animation;
            }
        }

        if (animation.getStatus() != Animation.Status.RUNNING) {
            animation.play();
        }

        return animation;
    }

    /**
     * Безопасно запускает анимацию и выполняет обработчик события при её подавлении.
     * <p>
     * Аналогичен {@link #play(Animation, String)}, но с дополнительной функцией:
     * если анимация подавляется (из-за свёрнутого окна или отсутствия фокуса),
     * переданный {@link Runnable} {@code eventHandler} немедленно выполняется.
     * </p>
     * <p>
     * Обработчик выполняется в потоке JavaFX Application Thread:
     * </p>
     * <ul>
     *   <li>Если текущий поток — JavaFX Application Thread,
     *       {@code eventHandler.run()} вызывается немедленно.</li>
     *   <li>Если другой поток — выполнение планируется через
     *       {@link Platform#runLater(Runnable)}.</li>
     * </ul>
     * <p>
     * <b>Важно:</b> Если анимация запускается успешно, {@code eventHandler} <b>не вызывается</b>.
     * Обработчик срабатывает только при подавлении анимации.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Animations.instance.play(fadeOut, "outTransition", () -> {
     *     node.setVisible(false);
     *     System.out.println("Элемент скрыт (анимация подавлена)");
     * });
     * }</pre>
     *
     * <h3>Поведение в зависимости от состояния</h3>
     * <pre>{@code
     * Состояние окна          | Действие
     * ------------------------|------------------------------------------
     * Свёрнуто                | stop(), eventHandler.run(), return
     * Не в фокусе + запрещено | stop(), eventHandler.run(), return
     * В фокусе / разрешено    | play() (если не запущена), return
     * }</pre>
     *
     * @param animation    объект анимации для запуска; не должен быть {@code null}
     * @param animationId  уникальный идентификатор анимации; используется для проверки
     *                     разрешённых анимаций, когда окно не в фокусе
     * @param eventHandler обработчик, который будет выполнен, если анимация подавлена;
     *                     может быть {@code null} (в этом случае ничего не выполняется)
     * @return переданный объект анимации, независимо от того, был ли он запущен
     * @throws NullPointerException если {@code animation} равен {@code null}
     * @see #play(Animation, String)
     * @see Platform#runLater(Runnable)
     * @see Platform#isFxApplicationThread()
     * @since 0.1.4.4
     */
    public Animation play(Animation animation, String animationId, Runnable eventHandler) {
        if (animation == null) {
            throw new NullPointerException("animation cannot be null");
        }

        if (isAppMinimized) {
            if (Platform.isFxApplicationThread()) {
                eventHandler.run();
            } else {
                Platform.runLater(eventHandler);
            }

            animation.stop();

            return animation;
        }

        if (!isAppFocused) {
            if (!allowedAnimationsWhenUnfocused.contains(animationId)) {
                if (Platform.isFxApplicationThread()) {
                    eventHandler.run();
                } else {
                    Platform.runLater(eventHandler);
                }

                animation.stop();

                return animation;
            }
        }

        if (animation.getStatus() != Animation.Status.RUNNING) {
            animation.play();
        }

        return animation;
    }

    /**
     * Инициализирует класс, привязывая его к жизненному циклу указанного окна.
     * <p>
     * Метод регистрирует слушатели на два ключевых свойства {@link Stage}:
     * </p>
     * <ul>
     *   <li>
     *     <b>Фокус</b>: слушатель на {@link Stage#focusedProperty()} обновляет поле
     *     {@link #isAppFocused} при изменении состояния фокуса окна.
     *   </li>
     *   <li>
     *     <b>Свёрнутость</b>: слушатель на {@link Stage#iconifiedProperty()} обновляет поле
     *     {@link #isAppMinimized} при сворачивании или разворачивании окна.
     *   </li>
     * </ul>
     * <p>
     * При повторном вызове метода с другим экземпляром {@link Stage} старые слушатели
     * автоматически удаляются из свойств предыдущего окна, что предотвращает утечку памяти.
     * </p>
     * <p>
     * Дополнительно регистрируется обработчик {@link Stage#setOnCloseRequest(javafx.event.EventHandler)},
     * который при закрытии окна:
     * </p>
     * <ol>
     *   <li>Удаляет слушатель фокуса из {@link Stage#focusedProperty()}.</li>
     *   <li>Удаляет слушатель свёрнутости из {@link Stage#iconifiedProperty()}.</li>
     *   <li>Обнуляет ссылку на окно ({@link #stageRef}).</li>
     * </ol>
     * <p>
     * <b>Рекомендация:</b> Вызовите этот метод один раз при запуске приложения, передав основное
     * окно ({@code primaryStage}). Повторный вызов допустим только при замене главного окна.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * @Override
     * public void start(Stage primaryStage) {
     *     // ... инициализация сцены
     *     Animations.instance.init(primaryStage);
     *     primaryStage.show();
     * }
     * }</pre>
     *
     * @param stage основное окно приложения; не должно быть {@code null}
     * @throws NullPointerException если {@code stage} равен {@code null}
     * @see #isAppFocused
     * @see #isAppMinimized
     * @see #stageRef
     * @see #focusListener
     * @see #iconifiedListener
     * @since 0.1.4.4
     */
    public void init(Stage stage) {
        if (stage == null) {
            throw new NullPointerException("stage cannot be null");
        }

        Stage oldStage = stageRef != null ? stageRef.get() : null;
        if (oldStage != null && !oldStage.equals(stage)) {
            oldStage.focusedProperty().removeListener(focusListener);
            oldStage.iconifiedProperty().removeListener(iconifiedListener);
        }

        stageRef = new WeakReference<>(stage);

        focusListener = (obs, oldVal, focused) -> isAppFocused = focused;
        iconifiedListener = (obs, oldVal, iconified) -> isAppMinimized = iconified;

        stage.focusedProperty().addListener(focusListener);
        stage.iconifiedProperty().addListener(iconifiedListener);

        stage.setOnCloseRequest(e -> {
            if (focusListener != null) {
                stage.focusedProperty().removeListener(focusListener);
            }
            if (iconifiedListener != null) {
                stage.iconifiedProperty().removeListener(iconifiedListener);
            }
            stageRef = null;
        });
    }
}