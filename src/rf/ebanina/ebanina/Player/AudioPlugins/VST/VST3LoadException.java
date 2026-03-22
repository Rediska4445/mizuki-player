package rf.ebanina.ebanina.Player.AudioPlugins.VST;

/**
 * <h1>VST3LoadException</h1>
 * Исключение, выбрасываемое при ошибках загрузки VST3 плагинов.
 * <p>
 * Возникает в следующих случаях:
 * </p>
 * <ul>
 *   <li>Не удается загрузить <code>editorhost.dll</code></li>
 *   <li>Некорректный VST3 формат (.vst3 папка повреждена)</li>
 *   <li>Ошибка инициализации JNI библиотеки <code>vst3</code></li>
 *   <li>Плагин несовместим с хост конфигурацией (sample rate, block size)</li>
 * </ul>
 *
 * <h3>Пример использования:</h3>
 * <pre>{@code
 * try {
 *     VST3 vst3 = new VST3(new File("MyPlugin.vst3"));
 * } catch (VST3LoadException e) {
 *     logger.error("Failed to load VST3: " + e.getMessage());
 *     // Показать пользователю список доступных плагинов
 * }
 * }</pre>
 *
 * <h3>Наследование:</h3>
 * <p>Расширяет {@link RuntimeException} для unchecked обработки ошибок загрузки.</p>
 *
 * @author Ebanina Std
 * @since 1.4.9
 * @see VST3
 * @see rf.vst3
 */
public class VST3LoadException
        extends RuntimeException
{
    /**
     * Создает исключение с стандартным сообщением.
     */
    public VST3LoadException() {
        super("VST-3 Load Exception");
    }
    /**
     * Создает исключение с пользовательским сообщением.
     *
     * @param message описание ошибки загрузки
     */
    public VST3LoadException(String message) {
        super(message);
    }
    /**
     * Создает исключение с сообщением и причиной.
     *
     * @param message описание ошибки
     * @param cause исходное исключение (IOException, UnsatisfiedLinkError)
     */
    public VST3LoadException(String message, Throwable cause) {
        super(message, cause);
    }
    /**
     * Создает исключение с причиной.
     *
     * @param cause исходное исключение
     */
    public VST3LoadException(Throwable cause) {
        super(cause);
    }
    /**
     * Создает исключение с расширенными настройками стека вызовов.
     *
     * @param message описание ошибки
     * @param cause исходное исключение
     * @param enableSuppression подавление связанных исключений
     * @param writableStackTrace возможность записи stack trace
     */
    protected VST3LoadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
