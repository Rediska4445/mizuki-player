package rf.ebanina.ebanina.KeyBindings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Контейнер для множественных {@link KeyBind} аннотаций.
 * <p>
 * Автоматически генерируется компилятором при использовании нескольких
 * {@code @KeyBind} на одном методе. Не используется напрямую.
 * </p>
 *
 * <pre>
 * @KeyBind(...)  // 1-я привязка
 * @KeyBind(...)  // 2-я привязка
 * // ≡ @KeyBinds({@KeyBind(...), @KeyBind(...)})
 * </pre>
 *
 * @see KeyBind
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface KeyBinds {
    /**
     * Массив всех KeyBind аннотаций на методе.
     */
    KeyBind[] value();
}