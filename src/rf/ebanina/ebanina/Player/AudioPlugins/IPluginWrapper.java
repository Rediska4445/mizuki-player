package rf.ebanina.ebanina.Player.AudioPlugins;

import java.nio.file.Path;
import java.util.Map;

/**
 * <h1>IPluginWrapper</h1>
 * Универсальная обертка для унификации VST/VST2/VST3 плагинов в аудио плеере.
 * <p>
 * Этот <b>генерик интерфейс</b> обеспечивает единый API для всех типов VST плагинов,
 * независимо от их SDK версии или спецификации. Разные реализации плагинов (VST2,
 * VST3, JNA-биндинги) приводятся к общему контракту, что позволяет плееру работать
 * с любыми плагинами через один интерфейс.
 * </p>
 *
 * <h3>Назначение</h3>
 * <ul>
 *   <li>Абстрагирование различий между VST2 и VST3 API</li>
 *   <li>Единая обработка параметров, I/O, GUI редакторов</li>
 *   <li>Сохранение/загрузка состояния плагина</li>
 *   <li>Управление жизненным циклом плагина (on/off/destroy)</li>
 * </ul>
 *
 * <h3>Генерик тип</h3>
 * <p>
 * <code>&lt;Plugin&gt;</code> — нативный тип плагина:
 * </p>
 * <ul>
 *   <li>VST2: <code>JVstHost$Plugin</code></li>
 *   <li>VST3: <code>Vst3Plugin</code></li>
 *   <li>JNA: нативный указатель</li>
 * </ul>
 *
 * <h3>Основной цикл обработки</h3>
 * <pre>{@code
 * IPluginWrapper&lt;VST3&gt; wrapper = new Vst3Wrapper(vst3Plugin);
 *
 * // В реальном времени (каждые 512 сэмплов)
 * wrapper.processReplacing(inputBuffer, outputBuffer, 512);
 *
 * // GUI
 * wrapper.openEditor();
 *
 * // Сохранение пресета
 * wrapper.save(presetPath, propsMap);
 * }</pre>
 *
 * <h3>Жизненный цикл плагина</h3>
 * <table border="1">
 *   <tr><th>Метод</th><th>Назначение</th></tr>
 *   <tr><td>{@link #turnOn()}</td><td>Инициализация/активация</td></tr>
 *   <tr><td>{@link #processReplacing(float[][], float[][], int)}</td><td>Обработка аудио</td></tr>
 *   <tr><td>{@link #openEditor()}</td><td>Открытие GUI</td></tr>
 *   <tr><td>{@link #turnOff()}</td><td>Деактивация</td></tr>
 *   <tr><td>{@link #destroy()}</td><td>Освобождение ресурсов</td></tr>
 * </table>
 *
 * @author Ebanina Std
 * @since 1.4.9
 * @param <Plugin> тип нативного VST плагина (VST2/VST3/JNA)
 * @see rf.ebanina.ebanina.Player.AudioPlugins.VST.VST
 * @see rf.ebanina.ebanina.Player.AudioPlugins.VST.VST3
 */
public interface IPluginWrapper<Plugin> {
    /**
     * Обработка аудио буфера <b>в режиме замещения</b> (VST стандарт).
     * <p>
     * Выполняется в <b>реальном времени</b> каждые 64-2048 сэмплов (buffer size).
     * Входной буфер полностью заменяется обработанным содержимым выходного буфера.
     * </p>
     *
     * <h3>Формат буферов:</h3>
     * <pre>{@code
     * vstInput[channels][framesRead]  → vstOutput[channels][framesRead]
     * диапазон: [-1.0f...+1.0f]
     * }</pre>
     *
     * <h3>Требования к реализации:</h3>
     * <ul>
     *   <li>Выходной буфер может совпадать с входным (<code>in-place</code>)</li>
     *   <li>Обработка &lt; 3мс (95th percentile)</li>
     *   <li>Детерминизм (одинаковый input → одинаковый output)</li>
     * </ul>
     *
     * @param vstInput входной многоканальный буфер
     * @param vstOutput выходной многоканальный буфер
     * @param framesRead количество сэмплов для обработки (обычно 512)
     */
    void processReplacing(float[][] vstInput, float[][] vstOutput, int framesRead);
    /**
     * Возвращает человекочитаемое название параметра плагина.
     * <p>
     * Используется для UI: слайдеры, названия в редакторе, автоматизация.
     * </p>
     *
     * @param i индекс параметра (0...{@link #numParameters()}-1)
     * @return название (например, "Cutoff Freq", "Dry/Wet", "Attack")
     */
    String getParameterName(int i);
    /**
     * Общее количество настраиваемых параметров плагина.
     * <p>
     * VST стандарт: обычно 10-100 параметров. 0 = плагин без параметров.
     * </p>
     *
     * @return количество параметров (0-256)
     */
    int numParameters();
    /**
     * Читает текущее значение параметра.
     * <p>
     * Нормализованное значение VST стандарта. Независимо от диапазона параметра.
     * </p>
     *
     * @param i индекс параметра
     * @return значение [0.0f...1.0f]
     */
    float getParameter(int i);
    /**
     * Устанавливает новое значение параметра.
     * <p>
     * <b>Thread-safe</b> вызов из UI и автоматизации. Плагин должен немедленно
     * отреагировать на изменение параметра во время {@link #processReplacing}.
     * </p>
     *
     * @param i индекс параметра
     * @param newVal новое значение [0.0f...1.0f]
     */
    void setParameter(int i, float newVal) ;
    /**
     * Деактивирует плагин (bypass/mute).
     * <p>
     * Плагин перестает обрабатывать аудио. Выходной буфер остается неизменным
     * или заполняется тишиной. Эквивалентно <code>setParameter(bypassIndex, 1.0f)</code>.
     * </p>
     */
    void turnOff();
    /**
     * Активирует плагин для обработки аудио.
     * <p>
     * Восстанавливает нормальную работу плагина. Эквивалентно
     * <code>setParameter(bypassIndex, 0.0f)</code>.
     * </p>
     */
    void turnOn();
    /**
     * Освобождает все ресурсы плагина и завершает его работу.
     * <p>
     * <b>Финальный метод жизненного цикла</b>. Вызывается при:
     * </p>
     * <ul>
     *   <li>Удалении плагина из цепочки эффектов</li>
     *   <li>Закрытии плеера</li>
     *   <li>Перезагрузке плагина</li>
     * </ul>
     * <p>
     * После вызова объект плагина <b>непригоден для использования</b>.
     * Освобождаются: DLL хэндлы, GUI окна, внутренние буферы, MIDI события.
     * </p>
     */
    void destroy();
    /**
     * Переоткрывает графический редактор плагина.
     * <p>
     * Полезно при:
     * <ul>
     *   <li>Изменении размера основного окна плеера</li>
     *   <li>Переключении между мониторами</li>
     *   <li>Восстановлении GUI после минимизации</li>
     * </ul>
     * </p>
     * <p>
     * Сначала закрывает существующее окно (если открыто), затем вызывает
     * {@link #openEditor()}. Гарантирует корректное обновление GUI.
     * </p>
     */
    void reOpenEditor();
    /**
     * Открывает графический редактор плагина (GUI).
     * <p>
     * VST стандартная функция. Создает и показывает окно редактора параметров.
     * Плагин <b>должен быть активирован</b> ({@link #turnOn()}) перед вызовом.
     * </p>
     * <p>
     * GUI взаимодействует с параметрами через {@link #setParameter} / {@link #getParameter}.
     * Поддерживает изменение параметров в реальном времени.
     * </p>
     */
    void openEditor();
    /**
     * Возвращает официальное название продукта плагина.
     * <p>
     * VST идентификатор продукта. Используется в UI для отображения в списке плагинов.
     * </p>
     * <p><b>Примеры:</b></p>
     * <ul>
     *   <li>"FabFilter Pro-Q 3"</li>
     *   <li>"Waves CLA-2A Compressor"</li>
     *   <li>"iZotope Ozone 11 Exciter"</li>
     * </ul>
     */
    String getProductString();
    /**
     * Возвращает название компании-разработчика.
     * <p>
     * VST vendor string. Отображается рядом с названием продукта в UI.
     * </p>
     * <p><b>Примеры:</b></p>
     * <ul>
     *   <li>"FabFilter"</li>
     *   <li>"Waves"</li>
     *   <li>"iZotope"</li>
     *   <li>"Steinberg Media Technologies"</li>
     * </ul>
     */
    String getVendorName();
    /**
     * Полный путь к DLL/SO файлу плагина на диске.
     * <p>
     * Абсолютный путь к исполняемому файлу плагина. Используется для:
     * </p>
     * <ul>
     *   <li>Отображения в UI (информационная панель)</li>
     *   <li>Перезагрузки плагина</li>
     *   <li>Сохранения в конфигурации</li>
     *   <li>Поиска обновлений</li>
     * </ul>
     * <p><b>Примеры:</b></p>
     * <pre>{@code
     * "C:\\Program Files\\Common Files\\VST3\\FabFilter\\Pro-Q 3.vst3"
     * "/Library/Audio/Plug-Ins/VST/ValhallaRoom.vst"
     * }</pre>
     */
    String getPluginPath();
    /**
     * Количество входных аудио каналов, поддерживаемых плагином.
     * <p>
     * VST конфигурация I/O. Определяет, сколько каналов плагин может принимать.
     * </p>
     * <p><b>Типичные значения:</b></p>
     * <ul>
     *   <li><code>0</code> — MIDI only (синтезатор)</li>
     *   <li><code>2</code> — стерео</li>
     *   <li><code>6</code> — 5.1 surround</li>
     *   <li><code>8</code> — 7.1 surround</li>
     * </ul>
     */
    int numInputs();
    /**
     * Количество выходных аудио каналов плагина.
     * <p>
     * Аналогично {@link #numInputs()}, но для выходных каналов.
     * Может отличаться от входных (например, моно → стерео).
     * </p>
     */
    int numOutputs();
    /**
     * Версия VST SDK, с которой совместим плагин.
     * <p>
     * Используется для определения поддерживаемых функций и поведения.
     * </p>
     * <p><b>Формат:</b></p>
     * <ul>
     *   <li><code>"VST2.4"</code></li>
     *   <li><code>"VST3.0"</code></li>
     *   <li><code>"VST3.6.7"</code></li>
     * </ul>
     */
    String getSdkVersion();
    /**
     * Сохраняет текущее состояние плагина (preset) в файл.
     * <p>
     * Сериализует все параметры, внутренние буферы, MIDI программы в файл.
     * Дополнительные метаданные записываются в <code>propsMap</code>.
     * </p>
     * <p><b>Форматы:</b> .fxp (VST2), .vstpreset (VST3)</p>
     *
     * @param path путь к файлу пресета (например, "MyReverb.vstpreset")
     * @param propsMap метаданные (название, автор, дата, комментарий)
     */
    void save(Path path, Map<String, String> propsMap);
    /**
     * Загружает состояние плагина из файла пресета.
     * <p>
     * Восстанавливает все параметры, буферы, настройки из файла.
     * Метаданные возвращаются в <code>out</code> Map.
     * </p>
     *
     * @param path путь к файлу пресета
     * @param out Map для метаданных (название, автор, версия)
     * @return <code>true</code> при успешной загрузке
     */
    boolean load(Path path, Map<String, String> out);
    /**
     * Расширение файлов состояния для данного плагина.
     * <p>
     * Основное расширение для сохранения пресетов.
     * </p>
     * <p><b>Примеры:</b></p>
     * <ul>
     *   <li>VST2: <code>".fxp"</code></li>
     *   <li>VST3: <code>".vstpreset"</code></li>
     * </ul>
     */
    String getStateExtension();
    /**
     * Массив поддерживаемых расширений файлов плагина.
     * <p>
     * Полный список форматов, которые может читать/записывать плагин.
     * </p>
     * <p><b>Примеры:</b></p>
     * <pre>{@code
     * VST2: [".fxp", ".fxb"]
     * VST3: [".vstpreset"]
     * }</pre>
     */
    String[] getPluginExtension();
    /**
     * Возвращает нативный объект VST плагина, обернутый этим wrapper'ом.
     * <p>
     * Предоставляет прямой доступ к исходному объекту плагина для:
     * </p>
     * <ul>
     *   <li>Расширенных операций (низкоуровневый доступ)</li>
     *   <li>Отладки и логирования</li>
     *   <li>Интеграции с другими системами</li>
     * </ul>
     * <p>
     * <b>Тип <code>Plugin</code>:</b>
     * </p>
     * <ul>
     *   <li>VST2: <code>JVstHost$Plugin</code></li>
     *   <li>VST3: <code>VST3$Plugin</code></li>
     *   <li>JNA: нативный указатель</li>
     * </ul>
     *
     * @return нативный объект плагина (не <code>null</code>)
     */
    Plugin getPlugin();
    /**
     * Устанавливает нативный объект VST плагина для обертки.
     * <p>
     * Используется при динамической загрузке плагинов или замене реализации.
     * Wrapper переинициализируется с новым нативным объектом.
     * </p>
     *
     * @param plugin нативный VST объект (не <code>null</code>)
     * @throws NullPointerException если <code>plugin</code> равен <code>null</code>
     */
    void setPlugin(Plugin plugin);
    /**
     * {@inheritDoc}
     * <p>
     * Возвращает строковое представление плагина.
     * </p>
     * <pre>{@code
     * "[ProductString] by [VendorName] (SDK: version) - path"
     * }</pre>
     * <p><b>Пример:</b></p>
     * <pre>{@code
     * "[Pro-Q 3] by FabFilter (VST3.6.7) - C:\VST3\Pro-Q 3.vst3"
     * }</pre>
     */
    @Override
    String toString();
    /**
     * {@inheritDoc}
     * <p>
     * Сравнивает два wrapper'а по <b>нативному плагину</b>.
     * Два wrapper'а равны, если обертывают один и тот же нативный объект.
     * </p>
     * <p>
     * Игнорирует различия в параметрах, состоянии GUI и других временных данных.
     * </p>
     *
     * @param o объект для сравнения
     * @return <code>true</code> если wrapper'ы ссылаются на один плагин
     */
    @Override
    boolean equals(Object o);
    /**
     * {@inheritDoc}
     * <p>
     * Хэш-код на основе нативного объекта плагина.
     * Гарантирует консистентность с {@link #equals(Object)}.
     * </p>
     * <p>
     * Два wrapper'а с одинаковым плагином имеют одинаковый hashCode.
     * </p>
     */
    @Override
    int hashCode();
}