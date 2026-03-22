package rf.ebanina.ebanina.Player.AudioPlugins.VST;

import com.synthbot.audioplugin.vst.vst2.JVstHost2;
import rf.ebanina.ebanina.Player.AudioPlugins.IPluginWrapper;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
/**
 * <h1>VST</h1>
 * Реализация {@link IPluginWrapper} для <b>VST2 плагинов</b> через библиотеку <code>JVstHost2</code>.
 * <p>
 * Предоставляет унифицированный доступ к VST2 API Steinberg через Java wrapper.
 * Обеспечивает полную совместимость с {@link PluginWrapper} системы плагинов плеера.
 * </p>
 *
 * <h3>Основные возможности:</h3>
 * <ul>
 *   <li>Обработка аудио в реальном времени ({@link #processReplacing})</li>
 *   *   <li>Управление параметрами (10-100 параметров типично)</li>
 *   <li>GUI редактор VST2</li>
 *   <li>Сохранение/загрузка пресетов (.fxp)</li>
 *   <li>Поддержка MIDI программ и bank chunks</li>
 * </ul>
 *
 * <h3>Специфика VST2:</h3>
 * <table border="1">
 *   <tr><th>Характеристика</th><th>Значение</th></tr>
 *   <tr><td>Формат</td><td>DLL (Windows), .vst (Mac)</td></tr>
 *   <tr><td>SDK</td><td>VST 2.4</td></tr>
 *   <tr><td>Параметры</td><td>0-256, normalized [0.0f...1.0f]</td></tr>
 *   <tr><td>Пресеты</td><td>.fxp (program), .fxb (bank)</td></tr>
 * </table>
 *
 * <h3>Создание и использование</h3>
 * <pre>{@code
 * // Загрузка VST2 плагина
 * JVstHost2 host = JVstHost2.newInstance(new File("C:\\VST\\Reverb.dll"));
 *
 * // Обертка для плеера
 * VST vstWrapper = new VST(host);
 * PluginWrapper plugin = new PluginWrapper(vstWrapper);
 *
 * // В аудио цикле
 * plugin.processReplacing(input, output, 512);
 * plugin.setParameter(0, 0.75f);  // Reverb wet
 * plugin.openEditor();            // GUI
 * }</pre>
 *
 * <h3>Особенности реализации:</h3>
 * <ul>
 *   <li><code>final</code> — не предназначен для наследования</li>
 *   <li>Полное делегирование к <code>JVstHost2</code></li>
 *   <li>Расширенное сохранение: все параметры + bank chunk + метаданные</li>
 *   <li>Сравнение по пути к DLL ({@link #equals})</li>
 * </ul>
 *
 * <h3>Ограничения VST2:</h3>
 * <ul>
 *   <li>32/64-bit DLL только</li>
 *   <li>Нет поддержки MIDI 2.0</li>
 *   <li>Устаревший SDK (заменен VST3)</li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 1.4.9
 * @see IPluginWrapper
 * @see JVstHost2
 * @see PluginWrapper.Type#VST
 * @implements IPluginWrapper&lt;JVstHost2&gt;
 */
public final class VST
        implements IPluginWrapper<JVstHost2>
{
    /**
     * Нативный VST2 хост через библиотеку <code>JVstHost2</code>.
     * <p>
     * Прямая ссылка на загруженный VST2 плагин. Может быть <code>null</code>
     * до инициализации или после {@link #destroy()}.
     * </p>
     */
    private JVstHost2 vst2Plugin;
    /**
     * Создает VST2 wrapper над загруженным плагином.
     * <p>
     * Инициализирует прямую ссылку на <code>JVstHost2</code> хост. Плагин
     * <b>должен быть предварительно загружен</b> через
     * <code>JVstHost2.newInstance(File)</code>.
     * </p>
     *
     * @param vst2Plugin загруженный VST2 хост (не <code>null</code>)
     */
    public VST(JVstHost2 vst2Plugin) {
        this.vst2Plugin = vst2Plugin;
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует <b>VST2 processReplacing</b> стандарт к <code>JVstHost2</code>.
     * Выполняется каждые 64-2048 сэмплов в реальном времени.
     * </p>
     */
    @Override
    public void processReplacing(float[][] vstInput, float[][] vstOutput, int framesRead) {
        vst2Plugin.processReplacing(vstInput, vstOutput, framesRead);
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST2 <code>getParameterName()</code>. Названия для UI слайдеров.
     * </p>
     */
    @Override
    public String getParameterName(int i) {
        return vst2Plugin.getParameterName(i);
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST2 <code>numParams</code>. Обычно 10-100 параметров.
     * </p>
     */
    @Override
    public int numParameters() {
        return vst2Plugin.numParameters();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST2 <code>getParameter()</code>. Нормализованное [0.0f...1.0f].
     * </p>
     */
    @Override
    public float getParameter(int i) {
        return vst2Plugin.getParameter(i);
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST2 <code>setParameter()</code>. Thread-safe изменение в реальном времени.
     * </p>
     */
    @Override
    public void setParameter(int i, float newVal) {
        vst2Plugin.setParameter(i, newVal);
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST2 <code>turnOff()</code> для деактивации обработки.
     * </p>
     */
    @Override
    public void turnOff() {
        vst2Plugin.turnOff();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST2 <code>turnOn()</code>. Активирует плагин для обработки аудио.
     * </p>
     */
    @Override
    public void turnOn() {
        vst2Plugin.turnOn();
    }
    /**
     * {@inheritDoc}
     * <p>
     * <b>VST2 специфика:</b> вызывает <code>turnOff()</code> вместо полного освобождения.
     * JVstHost2 управляет DLL хэндлом автоматически.
     * </p>
     */
    @Override
    public void destroy() {
        vst2Plugin.turnOff();
    }
    /**
     * {@inheritDoc}
     * <p>
     * VST2 <code>openEditor(vendorName)</code>. Пересоздает GUI редактор.
     * </p>
     */
    @Override
    public void reOpenEditor() {
        vst2Plugin.openEditor(getVendorName());
    }
    /**
     * {@inheritDoc}
     * <p>
     * VST2 <code>openEditor(vendorName)</code>. Открывает стандартный GUI редактор.
     * </p>
     */
    @Override
    public void openEditor() {
        vst2Plugin.openEditor(getVendorName());
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST2 <code>getEffectName()</code> или <code>getProductString()</code>.
     * </p>
     */
    @Override
    public String getProductString() {
        return vst2Plugin.getProductString();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST2 <code>getVendorString()</code>.
     * </p>
     */
    @Override
    public String getVendorName() {
        return vst2Plugin.getVendorName();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST2 <code>getPluginPath()</code> — полный путь к DLL.
     * </p>
     */
    @Override
    public String getPluginPath() {
        return vst2Plugin.getPluginPath();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST2 <code>numInputs()</code>. Количество входных каналов.
     * </p>
     */
    @Override
    public int numInputs() {
        return vst2Plugin.numInputs();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST2 <code>numOutputs()</code>. Количество выходных каналов.
     * </p>
     */
    @Override
    public int numOutputs() {
        return vst2Plugin.numOutputs();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Конвертирует VST2 <code>getVstVersion()</code> enum в строку.
     * Возвращает "VST2.4" или версию JVstHost2.
     * </p>
     */
    @Override
    public String getSdkVersion() {
        return vst2Plugin.getVstVersion().name();
    }
    /**
     * {@inheritDoc}
     * <p>
     * <b>VST2 расширенное сохранение:</b> серийализует <b>все</b> состояние плагина:
     * </p>
     * <ul>
     *   <li>Метаданные: тип, путь, I/O, название</li>
     *   <li><b>Все параметры:</b> name/value/label/display</li>
     *   <li><b>Bank chunk</b> (полное состояние программы)</li>
     *   <li>Хост настройки: sampleRate, blockSize, program</li>
     * </ul>
     * <p>Формат ключей: <code>"param.0.value" = "0.75"</code></p>
     */
    @Override
    public void save(Path path,  Map<String, String> propsMap) {
        try {
            propsMap.put("type", PluginWrapper.Type.VST.name());
            propsMap.put("pluginPath", getPluginPath());
            propsMap.put("productString", getProductString());
            propsMap.put("vendorName", getVendorName());
            propsMap.put("numInputs", String.valueOf(numInputs()));
            propsMap.put("numOutputs", String.valueOf(numOutputs()));

            propsMap.put("numParameters", String.valueOf(vst2Plugin.numParameters()));

            for (int i = 0; i < vst2Plugin.numParameters(); i++) {
                propsMap.put("param." + i + ".name", vst2Plugin.getParameterName(i));
                propsMap.put("param." + i + ".value", String.valueOf(vst2Plugin.getParameter(i)));
                propsMap.put("param." + i + ".label", vst2Plugin.getParameterLabel(i));
                propsMap.put("param." + i + ".display", vst2Plugin.getParameterDisplay(i));
            }

            propsMap.put("getBankChunk", Arrays.toString(vst2Plugin.getBankChunk()));
            propsMap.put("getEffectName", String.valueOf(vst2Plugin.getEffectName()));
            propsMap.put("getVstVersion", String.valueOf(vst2Plugin.getVstVersion()));
            propsMap.put("getProgram", String.valueOf(vst2Plugin.getProgram()));
            propsMap.put("canProcessReplacing", String.valueOf(vst2Plugin.canReplacing()));
            propsMap.put("getSampleRate", String.valueOf(vst2Plugin.getSampleRate()));
            propsMap.put("getBlockSize", String.valueOf(vst2Plugin.getBlockSize()));
            propsMap.put("getInputProperties", String.valueOf(vst2Plugin.getInputProperties(0)));
            propsMap.put("getOutputProperties", String.valueOf(vst2Plugin.getOutputProperties(0)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * {@inheritDoc}
     * <p>
     * <b>VST2 восстановление:</b> загружает только <b>значения параметров</b>.
     * Игнорирует метаданные (они используются {@link PluginWrapper#loadState}).
     * </p>
     * <p>Формат: <code>"param.0.value" → setParameter(0, 0.75f)</code></p>
     */
    @Override
    public boolean load(Path path, Map<String, String> out) {
        int numParams = Integer.parseInt(out.get("numParameters"));

        for (int i = 0; i < numParams && i < vst2Plugin.numParameters(); i++) {
            String valueStr = out.get("param." + i + ".value");

            if (valueStr != null) {
                float value = Float.parseFloat(valueStr);
                vst2Plugin.setParameter(i, value);
            }
        }

        return true;
    }
    /**
     * {@inheritDoc}
     * <p>
     * VST2 стандарт: возвращает <code>"steinberg"</code> из {@link PluginWrapper.Type#VST}.
     * </p>
     */
    @Override
    public String getStateExtension() {
        return PluginWrapper.Type.VST.stateExtension;
    }
    /**
     * {@inheritDoc}
     * <p>
     * VST2 стандарт: возвращает <code>["dll"]</code> из {@link PluginWrapper.Type#VST}.
     * </p>
     */
    @Override
    public String[] getPluginExtension() {
        return PluginWrapper.Type.VST.fileExtension;
    }
    /**
     * {@inheritDoc}
     * <p>
     * Возвращает нативный <code>JVstHost2</code> хост VST2 плагина.
     * </p>
     */
    @Override
    public JVstHost2 getPlugin() {
        return vst2Plugin;
    }
    /**
     * {@inheritDoc}
     * <p>
     * Динамически заменяет VST2 хост. Полезно при перезагрузке плагина.
     * </p>
     */
    @Override
    public void setPlugin(JVstHost2 jVstHost2) {
        this.vst2Plugin = jVstHost2;
    }
    /**
     * {@inheritDoc}
     * <p>
     * Отладочное представление: <code>"VST{vst2Plugin=[JVstHost2 object]}"</code>.
     * </p>
     */
    @Override
    public String toString() {
        return "VST{" +
                "vst2Plugin=" + vst2Plugin +
                '}';
    }
    /**
     * {@inheritDoc}
     * <p>
     * Сравнивает VST2 wrapper'ы по <b>пути к DLL файлу</b>.
     * Два плагина с одинаковым путем считаются идентичными.
     * </p>
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VST vst = (VST) o;
        return Objects.equals(vst2Plugin.getPluginPath(), vst.vst2Plugin.getPluginPath());
    }
    /**
     * {@inheritDoc}
     * <p>
     * Хэш-код на основе пути к DLL. Консистентен с {@link #equals(Object)}.
     * </p>
     */
    @Override
    public int hashCode() {
        return Objects.hash(vst2Plugin.getPluginPath());
    }
}