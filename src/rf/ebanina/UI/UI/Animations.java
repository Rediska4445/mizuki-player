package rf.ebanina.UI.UI;

import javafx.animation.Animation;
import javafx.stage.Stage;

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
public final class Animations {
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
    private static boolean isAppFocused = true;
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
    private static boolean isAppMinimized = false;
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
    private static final java.util.Set<String> allowedAnimationsWhenUnfocused = new java.util.HashSet<>(java.util.Arrays.asList(
           "outTransition", "outDropShadowAnimation", "inTransition", "inDropShadowAnimation"
    ));

    /**
     * Безопасно запускает анимацию в зависимости от текущего состояния приложения.
     * <p>
     * Это основной метод класса, который решает, должна ли анимация быть запущена.
     * Его поведение зависит от двух флагов:
     * <ol>
     *   <li><b>Свёрнуто</b>: если {@link #isAppMinimized} истинно, анимация <i>не запускается</i>.
     *       Метод сразу возвращает объект анимации.</li>
     *   <li><b>Не в фокусе</b>: если {@link #isAppFocused} ложно, проверяется, есть ли
     *       <code>animationId</code> в {@link #allowedAnimationsWhenUnfocused}. Если нет —
     *       анимация подавляется.</li>
     *   <li><b>В фокусе</b>: в любом другом случае анимация запускается с помощью {@link Animation#play()}.</li>
     * </ol>
     * </p>
     *
     * <h3>Пример логики</h3>
     * <pre>{@code
     * if (свёрнуто) -> return animation; // Не запускаем
     * if (не в фокусе && !разрешено) -> return animation; // Не запускаем
     * else -> animation.play(); return animation; // Запускаем
     * }</pre>
     *
     * @param animation объект анимации для запуска; не должен быть <code>null</code>
     * @param animationId уникальный идентификатор анимации (например, "fadeIn"); используется для фильтрации
     * @return переданный объект анимации, независимо от того, был ли он запущен
     * @throws NullPointerException если <code>animation</code> равен <code>null</code>
     * @see #play(Animation, String, Runnable)
     * @see Animation#play()
     * @since 0.1.4.4
     */
    public static Animation play(Animation animation, String animationId) {
        if (isAppMinimized) {
            return animation;
        }

        if (!isAppFocused) {
            if (!allowedAnimationsWhenUnfocused.contains(animationId)) {
                return animation;
            }
        }

        animation.play();

        return animation;
    }

    /**
     * Безопасно запускает анимацию и выполняет обработчик события при её подавлении.
     * <p>
     * Аналогичен {@link #play(Animation, String)}, но с дополнительной функцией:
     * если анимация подавляется (из-за состояния окна), переданный {@link Runnable}
     * <code>eventHandler</code> немедленно выполняется.
     * </p>
     * <p>
     * Это позволяет симулировать "завершение" анимации без её фактического запуска.
     * Например, если анимация исчезновения подавлена, обработчик может сразу
     * скрыть элемент и вызвать следующее действие.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Animations.play(fadeOut, "outTransition", () -> {
     *     node.setVisible(false);
     *     System.out.println("Элемент скрыт!");
     * });
     * }</pre>
     *
     * @param animation объект анимации для запуска; не должен быть <code>null</code>
     * @param animationId уникальный идентификатор анимации; используется для фильтрации
     * @param eventHandler обработчик, который будет выполнен, если анимация подавлена; может быть <code>null</code>
     * @return переданный объект анимации, независимо от того, был ли он запущен
     * @throws NullPointerException если <code>animation</code> равен <code>null</code>
     * @see #play(Animation, String)
     * @since 0.1.4.4
     */
    public static Animation play(Animation animation, String animationId, Runnable eventHandler) {
        if (isAppMinimized) {
            eventHandler.run();
            return animation;
        }

        if (!isAppFocused) {
            if (!allowedAnimationsWhenUnfocused.contains(animationId)) {
                eventHandler.run();
                return animation;
            }
        }

        animation.play();

        return animation;
    }

    /**
     * Инициализирует класс, привязывая его к жизненному циклу указанного окна.
     * <p>
     * Регистрирует слушатели на два ключевых свойства {@link Stage}:
     * <ul>
     *   <li><b>Фокус</b>: слушатель на {@link Stage#focusedProperty()} обновляет {@link #isAppFocused}.</li>
     *   <li><b>Свёрнутость</b>: слушатель на {@link Stage#iconifiedProperty()} обновляет {@link #isAppMinimized}.</li>
     * </ul>
     * </p>
     * <p>
     * Этот метод <b>должен</b> быть вызван один раз при запуске приложения,
     * перед использованием любых методов {@link #play(Animation, String)}.
     * </p>
     *
     * <h3>Типичное место вызова</h3>
     * <pre>{@code
     * public void start(Stage primaryStage) {
     *     // ... инициализация сцены
     *     Animations.init(primaryStage); // <- Здесь
     *     primaryStage.show();
     * }
     * }</pre>
     *
     * @param stage основное окно приложения; не должно быть <code>null</code>
     * @throws NullPointerException если <code>stage</code> равен <code>null</code>
     * @see #isAppFocused
     * @see #isAppMinimized
     * @since 0.1.4.4
     */
    public static void init(Stage stage) {
        stage.focusedProperty().addListener((obs, old, focused) -> {
            isAppFocused = focused;
        });

        stage.iconifiedProperty().addListener((obs, old, iconified) -> {
            isAppMinimized = iconified;
        });
    }
}