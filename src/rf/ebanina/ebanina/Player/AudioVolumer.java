package rf.ebanina.ebanina.Player;

import rf.ebanina.File.Resources.ResourceManager;

import java.io.File;

/**
 * <h1>AudioVolumer</h1>
 * Утилитарный класс для управления системной громкостью звука через JNI.
 * <p>
 * Предоставляет простой интерфейс для получения и установки системной громкости
 * Windows с использованием нативной библиотеки <code>VolumeLib.dll</code>.
 * Класс использует паттерн Singleton для единственного экземпляра с автоматической
 * загрузкой библиотеки при создании.
 * </p>
 * <p>
 * <b>Ключевые особенности:</b>
 * <ul>
 *   <li>Автоматическая загрузка нативной библиотеки из пути {@link ResourceManager#BIN_LIBRARIES_PATH}.</li>
 *   <li>Единственный статический экземпляр {@link #instance} для удобного доступа.</li>
 *   <li>Нативные методы работают с системным микшером Windows (Master Volume).</li>
 * </ul>
 * </p>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * // Получить текущую громкость (0.0f - 1.0f)
 * float currentVolume = AudioVolumer.instance.getSystemVolume();
 * System.out.println("Текущая громкость: " + (currentVolume * 100) + "%");
 *
 * // Установить громкость на 75%
 * AudioVolumer.instance.setSystemVolume(0.75f);
 * }</pre>
 *
 * <h3>Требования и ограничения</h3>
 * <ul>
 *   <li>Работает только на <b>Windows</b> (из-за <code>VolumeLib.dll</code>).</li>
 *   <li>Громкость принимает значения в диапазоне <code>[0.0f, 1.0f]</code>.</li>
 *   <li>Библиотека должна находиться в {@link ResourceManager#BIN_LIBRARIES_PATH}.</li>
 *   <li>При отсутствии библиотеки выбрасывается <code>UnsatisfiedLinkError</code>.</li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 0.1.4.11
 * @see ResourceManager#BIN_LIBRARIES_PATH
 */
public class AudioVolumer {
    /**
     * Конструктор загружает нативную библиотеку <code>VolumeLib.dll</code>.
     * <p>
     * Путь к библиотеке формируется как {@link ResourceManager#BIN_LIBRARIES_PATH}
     * + разделитель + <code>"VolumeLib.dll"</code>.
     * </p>
     * <p>
     * <b>Важно:</b> Вызывается только один раз при создании {@link #instance}.
     * При отсутствии библиотеки выбрасывается <code>UnsatisfiedLinkError</code>.
     * </p>
     *
     * @throws UnsatisfiedLinkError если библиотека <code>VolumeLib.dll</code> не найдена
     * @see System#load(String)
     * @see ResourceManager#BIN_LIBRARIES_PATH
     * @since 0.1.4.4
     */
    public AudioVolumer() {
        System.load(ResourceManager.BIN_LIBRARIES_PATH + File.separator + "VolumeLib.dll");
    }
    /**
     * Возвращает текущую системную громкость в диапазоне <code>[0.0f, 1.0f]</code>.
     * <p>
     * Значение <code>1.0f</code> соответствует 100% громкости, <code>0.0f</code> — полное
     * отключение звука. Метод обращается к системному микшеру Windows (Master Volume).
     * </p>
     *
     * <h3>Пример:</h3>
     * <pre>{@code
     * float volume = AudioVolumer.instance.getSystemVolume();
     * // volume = 0.75f (75% громкости)
     * }</pre>
     *
     * @return текущая системная громкость (0.0f - беззвучно, 1.0f - максимум)
     * @since 0.1.4.4
     */
    public native float getSystemVolume();
    /**
     * Устанавливает системную громкость звука.
     * <p>
     * Принимает значение в диапазоне <code>[0.0f, 1.0f]</code>, где:
     * </p>
     * <ul>
     *   <li><code>0.0f</code> — полное отключение звука (mute).</li>
     *   <li><code>1.0f</code> — максимальная громкость (100%).</li>
     *   <li>Любое промежуточное значение — линейная интерполяция.</li>
     * </ul>
     * <p>
     * Изменение применяется немедленно ко всем звуковым устройствам системы
     * (Master Volume). Значения вне диапазона могут привести к неопределённому поведению.
     * </p>
     *
     * <h3>Пример:</h3>
     * <pre>{@code
     * // Тихий звук (25%)
     * AudioVolumer.instance.setSystemVolume(0.25f);
     *
     * // Максимальная громкость
     * AudioVolumer.instance.setSystemVolume(1.0f);
     *
     * // Беззвучно
     * AudioVolumer.instance.setSystemVolume(0.0f);
     * }</pre>
     *
     * @param volume новая громкость в диапазоне [0.0f, 1.0f]
     * @since 0.1.4.4
     */
    public native void setSystemVolume(float volume);
    /**
     * Единственный экземпляр класса (Singleton).
     * <p>
     * Автоматически создаётся при первом обращении и загружает нативную библиотеку.
     * Готов к немедленному использованию без дополнительной инициализации.
     * </p>
     */
    public static AudioVolumer instance = new AudioVolumer();
}
