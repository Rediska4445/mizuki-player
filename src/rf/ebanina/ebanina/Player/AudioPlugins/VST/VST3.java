package rf.ebanina.ebanina.Player.AudioPlugins.VST;

import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.ebanina.Player.AudioPlugins.IPluginWrapper;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.vst3;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <h1>VST3</h1>
 * Реализация {@link IPluginWrapper} для <b>VST3 плагинов</b> через нативную библиотеку <code>vst3</code>.
 * <p>
 * Современная реализация VST3 с поддержкой <b>асинхронной инициализации</b>, многопоточности
 * и сложной <b>bus-архитектуры</b> (несколько аудио шин с переменным числом каналов).
 * </p>
 *
 * <h3>Ключевые отличия от VST2:</h3>
 * <ul>
 *   <li><b>VST3 формат:</b> папки .vst3 вместо DLL</li>
 *   <li><b>Bus система:</b> несколько входных/выходных шин (Audio/MIDI/Event)</li>
 *   <li><b>Асинхронность:</b> GUI, инициализация через executor</li>
 *   <li><b>64-bit обработка:</b> опциональная double precision</li>
 * </ul>
 *
 * <h3>Автоматическая загрузка:</h3>
 * <p>Использует встроенную библиотеку <code>editorhost.dll</code> из ресурсов:</p>
 * <pre>{@code ResourceManager.BIN_LIBRARIES_PATH + "/vst3/editorhost.dll"}</pre>
 *
 * <h3>Обработка bus-архитектуры:</h3>
 * <p>Автоматически конвертирует плоские float[][] буферы ↔ VST3 bus-структуру:</p>
 * <pre>{@code
 * stereo input[2][512] → bus0[2][512] (main audio bus)
 * stereo output[2][512] ← bus0[2][512]
 * }</pre>
 *
 * <h3>Асинхронная архитектура:</h3>
 * <table border="1">
 *   <tr><th>Операция</th><th>Метод</th></tr>
 *   <tr><td>Инициализация</td><td><code>asyncInit(...).get()</code></tr>
 *   <tr><td>GUI создание</td><td><code>asyncCreateView()</code></tr>
 *   <tr><td>GUI пересоздание</td><td><code>asyncReCreateView()</code></tr>
 *   <tr><td>Уничтожение</td><td><code>executor.submit(destroy)</code></tr>
 * </table>
 *
 * <h3>Пример использования:</h3>
 * <pre>{@code
 * VST3 vst3 = new VST3();  // Автозагрузка editorhost.dll
 * // или
 * VST3 vst3 = new VST3(new File("/VST3/MyPlugin.vst3"));
 *
 * PluginWrapper wrapper = new PluginWrapper(vst3);
 * wrapper.processReplacing(input, output, 512);
 * }</pre>
 *
 * <h3>Статус разработки:</h3>
 * <ul>
 *   <li>Аудио обработка с bus конвертацией</li>
 *   <li>Асинхронный GUI</li>
 *   <li>Сохранение/загрузка пресетов</li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 1.4.9
 * @see IPluginWrapper
 * @see PluginWrapper.Type#VST3
 * @implements IPluginWrapper&lt;vst3&gt;
 */
public final class VST3
        implements IPluginWrapper<vst3>
{
    /**
     * Нативный VST3 плагин через JNI библиотеку <code>vst3</code>.
     * <p>
     * Прямая ссылка на загруженный VST3 модуль. Может быть <code>null</code>
     * до инициализации или после {@link #destroy()}.
     * </p>
     */
    private vst3 vst3Plugin;
    /**
     * Встроенный dry/wet микс для VST3 (thread-safe).
     * <p>
     * Управляет балансом между входным и обработанным сигналом.
     * Диапазон: 0.0f (100% dry) ... 100.0f (100% wet).
     * Начальное значение: 100.0f (полностью обработанный сигнал).
     * </p>
     * <p>
     * Использует {@link AtomicReference} для безопасного доступа из UI и аудио потоков.
     * </p>
     */
    private final AtomicReference<Float> mix = new AtomicReference<>(100f);
    /**
     * Состояние активации VST3 плагина (thread-safe).
     * <p>
     * <code>true</code> — плагин обрабатывает аудио<br>
     * <code>false</code> — bypass режим (input копируется в output)
     * </p>
     * <p>
     * Использует {@link AtomicBoolean} для безопасного доступа из разных потоков.
     * Начальное значение: <code>false</code> (отключен).
     * </p>
     */
    private final AtomicBoolean isEnable = new AtomicBoolean(false);
    /**
     * Путь к встроенной VST3 хост библиотеке <code>editorhost.dll</code>.
     * <p>
     * Автоматическая загрузка из ресурсов приложения:
     * </p>
     * <pre>{@code ResourceManager.BIN_LIBRARIES_PATH + "/vst3/editorhost.dll"}</pre>
     */
    public static final String BIN_LIBRARIES_VST3 = ResourceManager.BIN_LIBRARIES_PATH + File.separator + "vst3" + File.separator + "editorhost.dll";
    /**
     * Создает VST3 wrapper с автозагрузкой встроенной библиотеки.
     * <p>
     * Использует <code>editorhost.dll</code> из ресурсов приложения.
     * </p>
     */
    public VST3() {
        this(new File(BIN_LIBRARIES_VST3));
    }
    /**
     * Создает VST3 wrapper с указанным файлом хоста.
     * <p>
     * Инициализирует JNI связь с VST3 хостом и отключает логирование.
     * </p>
     *
     * @param file путь к <code>editorhost.dll</code> или VST3 плагину
     */
    public VST3(File file) {
        vst3Plugin = new vst3(file);
        vst3Plugin.setLoggingEnable(false);
    }
    /**
     * {@inheritDoc}
     * <p>
     * <b>VST3 bus-архитектура:</b> конвертирует плоские float[][] буферы ↔ VST3 bus структуру.
     * </p>
     * <p><b>Алгоритм:</b></p>
     * <ol>
     *   <li>Вычисляет общее число каналов по всем input/output шинам</li>
     *   <li>Создает временные буферы нужного размера</li>
     *   <li>Копирует vstInput → tmpIn (дополняет нулями при нехватке каналов)</li>
     *   <li>Вызывает <code>vst3Plugin.process(tmpIn, tmpOut, framesRead)</code></li>
     *   <li>Копирует tmpOut → vstOutput (обрезает лишние каналы)</li>
     * </ol>
     * <p><b>Bypass:</b> при <code>isEnable=false</code> использует System.arraycopy</p>
     */
    @Override
    public void processReplacing(float[][] vstInput, float[][] vstOutput, int framesRead) {
        if(isEnable.get()) {
            int vstInBuses = vst3Plugin.getNumInputs();
            int vstOutBuses = vst3Plugin.getNumOutputs();

            int totalInChannels = 0;
            for (int i = 0; i < vstInBuses; i++) {
                totalInChannels += vst3Plugin.getNumChannelsForInputBus(i);
            }

            int totalOutChannels = 0;
            for (int i = 0; i < vstOutBuses; i++) {
                totalOutChannels += vst3Plugin.getNumChannelsForOutputBus(i);
            }

            float[][] tmpIn = new float[totalInChannels][framesRead];
            float[][] tmpOut = new float[totalOutChannels][framesRead];

            for (int ch = 0; ch < totalInChannels; ch++) {
                if (ch < vstInput.length) {
                    System.arraycopy(vstInput[ch], 0, tmpIn[ch], 0, framesRead);
                } else {
                    Arrays.fill(tmpIn[ch], 0, framesRead, 0f);
                }
            }

            vst3Plugin.process(tmpIn, tmpOut, framesRead);

            for (int ch = 0; ch < totalOutChannels; ch++) {
                if (ch < vstOutput.length) {
                    System.arraycopy(tmpOut[ch], 0, vstOutput[ch], 0, framesRead);
                }
            }
        } else {
            System.arraycopy(vstInput, 0, vstOutput, 0, vstInput.length);
        }
    }
    /**
     * {@inheritDoc}
     * <p>
     * <b>Заглушка:</b> поддержка параметров VST3 не реализована.
     * </p>
     */
    @Override
    public String getParameterName(int i) {
        return null;
    }
    /**
     * {@inheritDoc}
     * <p>
     * <b>Заглушка:</b> возвращает 0. Параметры VST3 не поддерживаются.
     * </p>
     */
    @Override
    public int numParameters() {
        return 0;
    }
    /**
     * {@inheritDoc}
     * <p>
     * <b>Заглушка:</b> возвращает 0.0f. Параметры не поддерживаются.
     * </p>
     */
    @Override
    public float getParameter(int i) {
        return 0;
    }
    /**
     * {@inheritDoc}
     * <p>
     * <b>Не реализовано:</b> управление параметрами VST3 требует доработки.
     * </p>
     */
    @Override
    public void setParameter(int i, float newVal) {
        // TODO
    }
    /**
     * {@inheritDoc}
     * <p>
     * Отключает обработку VST3. Переводит в bypass режим.
     * </p>
     */
    @Override
    public void turnOff() {
        isEnable.set(false);
    }
    /**
     * {@inheritDoc}
     * <p>
     * Включает обработку VST3 аудио.
     * </p>
     */
    @Override
    public void turnOn() {
        isEnable.set(true);
    }
    /**
     * {@inheritDoc}
     * <p>
     * Асинхронное уничтожение VST3 плагина через executor.
     * Выполняется только если плагин активен ({@link #isEnable}).
     * </p>
     */
    @Override
    public void destroy() {
        if(isEnable.get()) {
            vst3Plugin.executor.submit(() -> vst3Plugin.destroy());
        }
    }
    /**
     * {@inheritDoc}
     * <p>
     * Асинхронное пересоздание VST3 GUI редактора.
     * Выполняется только при активном плагине.
     * </p>
     */
    @Override
    public void reOpenEditor() {
        if(isEnable.get()) {
            vst3Plugin.asyncReCreateView();
        }
    }
    /**
     * {@inheritDoc}
     * <p>
     * Асинхронное создание VST3 GUI редактора.
     * Выполняется только при активном плагине.
     * </p>
     */
    @Override
    public void openEditor() {
        if(isEnable.get()) {
            vst3Plugin.asyncCreateView();
        }
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST3 <code>getPluginName()</code>.
     * </p>
     */
    @Override
    public String getProductString() {
        return vst3Plugin.getPluginName();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST3 <code>getVendor()</code>.
     * </p>
     */
    @Override
    public String getVendorName() {
        return vst3Plugin.getVendor();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Возвращает абсолютный путь к файлу хоста VST3 (<code>editorhost.dll</code>).
     * </p>
     */
    @Override
    public String getPluginPath() {
        return vst3Plugin.plugin.getAbsolutePath();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Нормализует количество входных шин VST3. Моно шины (&le;1) преобразуются в стерео (2).
     * </p>
     */
    @Override
    public int numInputs() {
        return vst3Plugin.getNumInputs() <= 1 ? 2 : vst3Plugin.getNumInputs();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Нормализует количество выходных шин VST3. Моно шины (&le;1) преобразуются в стерео (2).
     * </p>
     */
    @Override
    public int numOutputs() {
        return vst3Plugin.getNumOutputs() <= 1 ? 2 : vst3Plugin.getNumOutputs();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует VST3 <code>getSdkVersion()</code>.
     * </p>
     */
    @Override
    public String getSdkVersion() {
        return vst3Plugin.getSdkVersion();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Сохраняет метаданные VST3 + все параметры плагина.
     * Формат: <code>"param.0.value" = "0.75f"</code>.
     * </p>
     */
    @Override
    public void save(Path path, Map<String, String> propsMap) {
        propsMap.put("type", PluginWrapper.Type.VST3.name());
        propsMap.put("pluginPath", getPluginPath());
        propsMap.put("productString", getProductString());
        propsMap.put("vendorName", getVendorName());
        propsMap.put("isEnable", String.valueOf(isEnable.get()));
        propsMap.put("numInputs", String.valueOf(numInputs()));
        propsMap.put("numOutputs", String.valueOf(numOutputs()));

        try {
            propsMap.put("numParameters", String.valueOf(vst3Plugin.getParameterCount()));

            for (int i = 0; i < vst3Plugin.getParameterCount(); i++) {
                propsMap.put("param." + i + ".value", String.valueOf(vst3Plugin.getParameterValue(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * {@inheritDoc}
     * <p>
     * <b>Полная переинициализация VST3:</b>
     * </p>
     * <ol>
     *   <li>Пересоздает <code>vst3</code> экземпляр</li>
     *   <li>Устанавливает callback восстановления параметров</li>
     *   <li><b>Асинхронная инициализация</b> с конфигурацией хоста</li>
     * </ol>
     */
    @Override
    public boolean load(Path path, Map<String, String> out) {
        vst3Plugin = new vst3(new File(BIN_LIBRARIES_VST3));
        vst3Plugin.setLoggingEnable(false);

        vst3Plugin.setOnInitialize(() -> {
            int numParams = Integer.parseInt(out.get("numParameters"));

            for (int i = 0; i < numParams && i < vst3Plugin.getParameterCount(); i++) {
                String valueStr = out.get("param." + i + ".value");

                if (valueStr != null) {
                    vst3Plugin.setParameterValue(i, Float.parseFloat(valueStr));
                }
            }
        });

        try {
            if(!vst3Plugin.asyncInit(
                    new File(out.get("pluginPath")),
                    ConfigurationManager.instance.getIntItem("vst_sample_rate", String.valueOf((int) MediaProcessor.mediaProcessor.mediaPlayer.getSampleRate())),
                    ConfigurationManager.instance.getIntItem("vst_max_block_size", String.valueOf(MediaProcessor.mediaProcessor.MEDIA_PLAYER_BLOCK_SIZE_FRAMES)),
                    ConfigurationManager.instance.getIntItem("vst3_double_64_processing", "0"),
                    ConfigurationManager.instance.getBooleanItem("vst3_is_real_time_processing", "true")
            ).get()) {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }
    /**
     * {@inheritDoc}
     * <p>
     * VST3 использует расширение VST2 (<code>"steinberg"</code>) для совместимости.
     * </p>
     */
    @Override
    public String getStateExtension() {
        return PluginWrapper.Type.VST3.stateExtension;
    }
    /**
     * {@inheritDoc}
     * <p>
     * VST3 расширения файлов: <code>["vst3"]</code>.
     * </p>
     */
    @Override
    public String[] getPluginExtension() {
        return PluginWrapper.Type.VST3.fileExtension;
    }
    /**
     * {@inheritDoc}
     * <p>
     * Возвращает нативный VST3 объект JNI библиотеки.
     * </p>
     */
    @Override
    public vst3 getPlugin() {
        return vst3Plugin;
    }
    /**
     * {@inheritDoc}
     * <p>
     * Динамически заменяет VST3 плагин. Полезно при перезагрузке.
     * </p>
     */
    @Override
    public void setPlugin(vst3 vst3) {
        this.vst3Plugin = vst3;
    }
    /**
     * {@inheritDoc}
     * <p>
     * Отладочное представление с полным состоянием VST3 wrapper'а.
     * </p>
     */
    @Override
    public String toString() {
        return "VST3{" +
                "vst3Plugin=" + vst3Plugin +
                ", mix=" + mix +
                ", isEnable=" + isEnable +
                '}';
    }
    /**
     * {@inheritDoc}
     * <p>
     * Полное сравнение состояния: плагин + {@link #mix} + {@link #isEnable}.
     * </p>
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VST3 vst3 = (VST3) o;
        return Objects.equals(vst3Plugin, vst3.vst3Plugin) && Objects.equals(mix, vst3.mix) && Objects.equals(isEnable, vst3.isEnable);
    }
    /**
     * {@inheritDoc}
     * <p>
     * Хэш-код на основе пути к VST3 хост файлу.
     * </p>
     */
    @Override
    public int hashCode() {
        return Objects.hash(vst3Plugin.plugin.getAbsolutePath());
    }
}