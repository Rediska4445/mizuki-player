package rf.ebanina.File.Configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация, указывающая, что поле конфигурации подлежит инициализации из файла конфигурации.
 * <p>
 * Позволяет задать ключ для поиска значения в конфигурации и значение по умолчанию,
 * если параметр отсутствует.
 * </p>
 * <p>
 * Пример использования:
 * </p>
 * <pre>{@code
 * public class MySettings {
 *     @ConfigurableField(key = "app.timeout", ifNull = "30")
 *     public static int timeout;
 * }
 * }</pre>
 *
 * @author Ebanina Std
 * @version 0.1.4.6
 * @since 0.1.1.5
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigurableField {
    /**
     * Ключ в конфигурационном файле, по которому искать значение.
     * Если не задан, используется имя поля.
     */
    String key() default "null_field";
    /**
     * Значение по умолчанию, если конфигурационный параметр отсутствует.
     */
    String ifNull() default "";
}
