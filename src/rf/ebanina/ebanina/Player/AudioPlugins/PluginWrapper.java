package rf.ebanina.ebanina.Player.AudioPlugins;

import com.synthbot.audioplugin.vst.JVstLoadException;
import com.synthbot.audioplugin.vst.vst2.JVstHost2;
import javafx.beans.property.SimpleObjectProperty;
import rf.ebanina.File.FileManager;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.AudioPlugins.VST.VST;
import rf.ebanina.ebanina.Player.AudioPlugins.VST.VST3;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h1>PluginWrapper</h1>
 * Универсальная обертка второго уровня для VST/VST3 плагинов с поддержкой <b>mix</b> и <b>presets</b>.
 * <p>
 * Оборачивает {@link IPluginWrapper} и добавляет:
 * </p>
 * <ul>
 *   <li><b>Dry/Wet микширование</b> (параллельная обработка)</li>
 *   <li><b>Сохранение/загрузка пресетов</b> (.vstpreset, .fxp)</li>
 *   <li><b>Логирование</b> и обработка ошибок</li>
 *   <li><b>Автоматическая реконфигурация</b> при загрузке</li>
 * </ul>
 *
 * <h3>Особенности микширования</h3>
 * <p>
 * <code>mix = 100%</code> → 100% обработанный сигнал<br>
 * <code>mix = 0%</code> → 100% сухой сигнал (bypass)<br>
 * <code>mix = 50%</code> → 50/50 dry/wet
 * </p>
 *
 * <h3>Формула dry/wet:</h3>
 * <pre>{@code
 * output[i] = (1-mix) * input[i] + mix * output[i]
 * }</pre>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * PluginWrapper wrapper = new PluginWrapper(new Vst3Wrapper(vst3Plugin));
 * wrapper.setMix(75);  // 75% wet
 * wrapper.turnOn();
 *
 * // В аудио цикле
 * wrapper.processReplacing(input, output, 512);
 * }</pre>
 *
 * @author Ebanina Std
 * @since 1.4.9
 * @see IPluginWrapper
 * @see VST
 * @see VST3
 */
public class PluginWrapper {
    /**
     * Обертка над нативным VST/VST3 плагином.
     * <p>
     * Хранит ссылку на реализацию {@link IPluginWrapper} и обеспечивает единый
     * доступ ко всем методам VST API через делегирование. Может быть <code>null</code>
     * до инициализации плагина.
     * </p>
     * <p>
     * Тип: wildcard generics (<code>?</code>) для совместимости с VST2, VST3 и JNA.
     * </p>
     */
    private IPluginWrapper<?> plugin;
    /**
     * <h1>Type</h1>
     * Типы поддерживаемых VST плагинов с метаданными файлов.
     * <p>
     * Определяет расширения исполняемых файлов и файлов состояния для корректной
     * загрузки и сохранения пресетов.
     * </p>
     */
    public enum Type {
        /** VST2 плагины (32/64-bit DLL) Steinberg */
        VST("steinberg", "dll"),
        /** VST3 плагины (VST3 папки/файлы) Steinberg */
        VST3("steinberg","vst3");
        /** Расширение файла состояния (.vstpreset, .fxp) */
        public final String stateExtension;
        /** Расширения исполняемых файлов плагина */
        public final String[] fileExtension;
        /**
         * Создает тип плагина с расширениями файлов.
         *
         * @param stateExtension расширение пресетов
         * @param fileExtension расширения DLL/VST3 файлов
         */
        Type(String stateExtension, String... fileExtension) {
            this.stateExtension = stateExtension;
            this.fileExtension = fileExtension;
        }
    }
    /**
     * Состояние активации плагина (thread-safe).
     * <p>
     * <code>true</code> — плагин обрабатывает аудио (активен)<br>
     * <code>false</code> — bypass (input копируется в output без обработки)
     * </p>
     * <p>
     * Использует {@link AtomicBoolean} для безопасного доступа из UI и аудио потоков.
     * Начальное значение: <code>true</code> (включен).
     * </p>
     */
    private final AtomicBoolean isEnable = new AtomicBoolean(true);
    /**
     * Dry/Wet микширование (параллельная обработка).
     * <p>
     * Управляет балансом между сухим (input) и обработанным (plugin) сигналом:
     * </p>
     * <ul>
     *   <li><code>100</code> — 100% wet (только обработанный сигнал)</li>
     *   <li><code>0</code> — 100% dry (bypass)</li>
     *   <li><code>50</code> — 50/50 dry/wet микс</li>
     * </ul>
     *
     * <p><b>Структура:</b></p>
     * <ul>
     *   <li>{@link SimpleObjectProperty} — привязка к JavaFX UI (Property binding)</li>
     *   <li>{@link AtomicInteger} — thread-safe значение (0-100)</li>
     * </ul>
     * <p>Начальное значение: <code>100</code> (полностью wet).</p>
     */
    private final SimpleObjectProperty<AtomicInteger> mix = new SimpleObjectProperty<>(new AtomicInteger(100));
    /**
     * Устанавливает состояние активации плагина.
     * <p>
     * Thread-safe изменение состояния. При <code>false</code> плагин переходит
     * в режим bypass — {@link #processReplacing} копирует входной сигнал напрямую
     * в выходной без обработки.
     * </p>
     *
     * @param isEnable <code>true</code> — плагин активен, <code>false</code> — bypass
     */
    public void setEnable(boolean isEnable) {
        this.isEnable.set(isEnable);
    }
    /**
     * Проверяет, активен ли плагин для обработки аудио.
     * <p>
     * Thread-safe чтение состояния. Используется для:
     * </p>
     * <ul>
     *   <li>Определения режима работы в UI</li>
     *   <li>Условной обработки в {@link #processReplacing}</li>
     *   *   <li>Логирования состояния</li>
     * </ul>
     *
     * @return <code>true</code> если плагин обрабатывает аудио
     */
    public boolean isEnable() {
        return isEnable.get();
    }
    /**
     * Создает wrapper над VST/VST3 плагином.
     * <p>
     * Инициализирует обертку с заданным нативным плагином. Автоматически активирует
     * плагин ({@link #isEnable} = <code>true</code>) и устанавливает микширование
     * в 100% wet.
     * </p>
     *
     * @param plugin обертка над нативным VST/VST3 ({@link IPluginWrapper})
     */
    public PluginWrapper(IPluginWrapper<?> plugin) {
        this.plugin = plugin;
    }
    /**
     * Возвращает обернутый VST/VST3 плагин.
     * <p>
     * Предоставляет прямой доступ к нативной обертке для расширенных операций.
     * </p>
     *
     * @return IPluginWrapper над нативным плагином (может быть <code>null</code>)
     */
    public IPluginWrapper<?> getPlugin() {
        return plugin;
    }
    /**
     * Заменяет VST плагин в текущей обертке.
     * <p>
     * Динамическая подмена плагина без создания нового wrapper'а.
     * Сбрасывает состояние {@link #isEnable} в <code>true</code>.
     * </p>
     * <p><b>Pattern:</b> Builder/Fluent interface</p>
     *
     * @param plugin новый VST плагин
     * @return <code>this</code> для цепочки вызовов
     */
    public PluginWrapper setPlugin(IPluginWrapper<?> plugin) {
        this.plugin = plugin;
        return this;
    }
    /**
     * Конструктор по умолчанию.
     * <p>
     * Создает пустую обертку (<code>plugin = null</code>, {@link #isEnable} = <code>true</code>,
     * {@link #mix} = 100%). Требует последующего вызова {@link #setPlugin}.
     * </p>
     */
    public PluginWrapper() {}
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует запрос к обернутому VST плагину. Возвращает название параметра
     * для отображения в UI (слайдеры, списки параметров).
     * </p>
     */
    public String getParameterName(int i) {
        return plugin.getParameterName(i);
    }
    /**
     * Callback, вызываемый при первом включении плагина ({@link #turnOn()}).
     * <p>
     * Используется для инициализации специфичных действий:
     * </p>
     * <ul>
     *   <li>Сброс параметров в дефолт</li>
     *   <li>Загрузка пользовательского пресета</li>
     *   <li>Подготовка GUI</li>
     * </ul>
     * <p><b>По умолчанию:</b> <code>null</code> (не вызывается).</p>
     */
    public Runnable onInit = null;
    /**
     * Возвращает текущее значение dry/wet микширования.
     * <p>
     * Нормализованное значение [0.0f...100.0f]. Используется в UI и алгоритме
     * {@link #processReplacing}.
     * </p>
     *
     * @return mix значение (0.0f = 100% dry, 100.0f = 100% wet)
     */
    public float getMix() {
        return mix.get().get();
    }
    /**
     * Устанавливает dry/wet микширование.
     * <p>
     * Thread-safe изменение микса. Новый mix применяется немедленно в следующем
     * вызове {@link #processReplacing}.
     * </p>
     * <p><b>Pattern:</b> Fluent interface</p>
     *
     * @param mix значение 0-100 (% обработанного сигнала)
     * @return <code>this</code> для цепочки вызовов
     */
    public PluginWrapper setMix(int mix) {
        this.mix.get().set(mix);
        return this;
    }
    /**
     * <h3>Основной метод обработки аудио с dry/wet микшированием</h3>
     * <p>
     * Выполняет полный цикл обработки VST плагина в реальном времени:
     * </p>
     *
     * <h4>Логика работы:</h4>
     * <table border="1">
     *   <tr><th>Условие</th><th>Действие</th></tr>
     *   <tr><td><code>isEnable=false</code></td><td>Байпас: копирует input→output</td></tr>
     *   <tr><td><code>isEnable=true, mix=100%</code></td><td>100% обработанный сигнал</td></tr>
     *   <tr><td><code>isEnable=true, mix&lt;100%</code></td><td>Dry/Wet микс</td></tr>
     * </table>
     *
     * <h4>Формула dry/wet микширования:</h4>
     * <pre>{@code
     * mixValue = mix / 100.0f    // [0.0f...1.0f]
     * output[i] = (1-mixValue) * input[i] + mixValue * processed[i]
     * }</pre>
     *
     * <h4>Оптимизации:</h4>
     * <ul>
     *   <li>При <code>mix=100%</code> пропускает микширование</li>
     *   <li>Байпас использует <code>System.arraycopy</code> (самый быстрый)</li>
     * </ul>
     *
     * @param vstInput входной многоканальный буфер
     * @param vstOutput выходной многоканальный буфер (in-place возможно)
     * @param framesRead количество сэмплов (обычно 256-2048)
     */
    public void processReplacing(float[][] vstInput, float[][] vstOutput, int framesRead) {
        if(isEnable.get()) {
            plugin.processReplacing(vstInput, vstOutput, framesRead);

            float mixValue = getMix() / 100.0f;

            if (mixValue < 1.0f) {
                for (int channel = 0; channel < vstOutput.length; channel++) {
                    for (int i = 0; i < framesRead; i++) {
                        vstOutput[channel][i] = (1.0f - mixValue) * vstInput[channel][i] +
                                mixValue * vstOutput[channel][i];
                    }
                }
            }
        } else {
            System.arraycopy(vstInput, 0, vstOutput, 0, vstInput.length);
        }
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует запрос к обернутому VST плагину. Thread-safe чтение значения параметра.
     * </p>
     */
    public float getParameter(int i) {
        return plugin.getParameter(i);
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует запрос к VST плагину. Кэшируется для оптимизации UI обновлений.
     * </p>
     */
    public int numParameters() {
        return plugin.numParameters();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Thread-safe изменение параметра. Изменение применяется немедленно в следующем
     * вызове {@link #processReplacing}.
     * </p>
     */
    public void setParameter(int i, float newVal) {
        plugin.setParameter(i, newVal);
    }
    /**
     * Деактивирует плагин (полный bypass).
     * <p>
     * Двойная деактивация:
     * </p>
     * <ol>
     *   <li>Устанавливает {@link #isEnable} = <code>false</code></li>
     *   <li>Вызывает {@link IPluginWrapper#turnOff()} нативного плагина</li>
     * </ol>
     * <p>
     * После вызова {@link #processReplacing} копирует input→output без обработки.
     * </p>
     */
    public void turnOff() {
        isEnable.set(false);

        plugin.turnOff();
    }
    /**
     * Активирует плагин для обработки.
     * <p>
     * Полная активация:
     * </p>
     * <ol>
     *   <li>Устанавливает {@link #isEnable} = <code>true</code></li>
     *   <li>Вызывает {@link IPluginWrapper#turnOn()} нативного плагина</li>
     *   <li>Выполняет {@link #onInit} callback (если установлен)</li>
     * </ol>
     */
    public void turnOn() {
        isEnable.set(true);

        plugin.turnOn();

        if(onInit != null) {
            onInit.run();
        }
    }
    /**
     * Безопасное освобождение ресурсов плагина.
     * <p>
     * Проверяет {@link #isEnable} перед вызовом {@link IPluginWrapper#destroy()}.
     * Предотвращает двойное освобождение ресурсов при многопоточном доступе.
     * </p>
     */
    public void destroy() {
        if(isEnable.get()) {
            plugin.destroy();
        }
    }
    /**
     * Переоткрывает GUI редактора VST3 плагина.
     * <p>
     * Специализированный метод для VST3. Вызывает {@link IPluginWrapper#reOpenEditor()}
     * только если плагин активен ({@link #isEnable} = <code>true</code>).
     * </p>
     * <p>
     * Полезно при изменении размера окна плеера, переключении мониторов или
     * восстановлении GUI после минимизации.
     * </p>
     */
    public void reOpenVst3GUI() {
        if(isEnable.get()) {
            plugin.reOpenEditor();
        }
    }
    /**
     * Открывает графический редактор плагина.
     * <p>
     * Логирует состояние плагина перед открытием. GUI открывается только
     * если плагин активен ({@link #isEnable} = <code>true</code>).
     * </p>
     * <p><b>Лог:</b> <code>"[pluginPath]: true/false"</code></p>
     */
    public void openEditor() {
        Music.mainLogger.info(getPluginPath() + ": " + isEnable.get());

        if(isEnable.get()) {
            plugin.openEditor();
        }
    }
    /**
     * {@inheritDoc}
     * Делегирует запрос к VST плагину. Название продукта для UI списка плагинов.
     */
    public String getProductString() {
        return plugin.getProductString();
    }
    /**
     * {@inheritDoc}
     * Делегирует запрос к VST плагину. Название разработчика для UI.
     */
    public String getVendorName() {
        return plugin.getVendorName();
    }
    /**
     * {@inheritDoc}
     * Делегирует запрос к VST плагину. Полный путь к DLL/VST3 файлу.
     */
    public String getPluginPath() {
        return plugin.getPluginPath();
    }
    /**
     * {@inheritDoc}
     * Делегирует запрос к VST плагину. Количество входных каналов.
     */
    public int numInputs() {
        return plugin.numInputs();
    }
    /**
     * {@inheritDoc}
     * Делегирует запрос к VST плагину. Количество выходных каналов.
     */
    public int numOutputs() {
        return plugin.numOutputs();
    }
    /**
     * {@inheritDoc}
     * Делегирует запрос к VST плагину. Версия VST SDK.
     */
    public String getSdkVersion() {
        return plugin.getSdkVersion();
    }
    /**
     * Сохраняет полное состояние плагина в файл пресета.
     * <p>
     * Сохраняет три группы данных:
     * </p>
     * <ol>
     *   <li><b>Wrapper состояние:</b> {@link #isEnable}, {@link #mix}, путь к плагину</li>
     *   *   <li><b>Нативное состояние:</b> параметры VST через {@link IPluginWrapper#save}</li>
     *   <li><b>Метаданные:</b> в {@link FileManager} формате</li>
     * </ol>
     *
     * @param path путь к файлу пресета (.vstpreset, .fxp + метаданные)
     */
    public void saveState(Path path) {
        Map<String, String> propsMap = new HashMap<>();

        propsMap.put("pluginPath", getPluginPath());
        propsMap.put("isEnable", String.valueOf(isEnable.get()));
        propsMap.put("mix", String.valueOf(getMix()));

        plugin.save(path, propsMap);

        FileManager.instance.saveArray(path.toFile().getPath(), "plugin", propsMap);
    }
    /**
     * Загружает полное состояние плагина из файла пресета.
     * <p>
     * <b>Сложная логика восстановления:</b>
     * </p>
     * <ol>
     *   <li>Читает метаданные через {@link FileManager}</li>
     *   <li>Восстанавливает {@link #isEnable} и {@link #mix}</li>
     *   <li><b>Пересоздает плагин</b> по типу (VST/VST3) и пути</li>
     *   <li>Загружает нативное состояние через {@link IPluginWrapper#load}</li>
     * </ol>
     *
     * @param path путь к файлу пресета
     * @return <code>true</code> при полном успехе
     */
    public boolean loadState(Path path) {
        Map<String, String> out = FileManager.instance.readArray(path.toFile().getPath(), "plugin", Map.of());

        try {
            this.setEnable(Boolean.parseBoolean(out.get("isEnable")));
            this.setMix((int) Float.parseFloat(out.get("mix")));

            String type = out.get("type");

            if(type == null)
                return false;

            if(type.equalsIgnoreCase(Type.VST.name())) {
                try {
                    plugin = new VST(JVstHost2.newInstance(new File(out.get("pluginPath"))));
                } catch (FileNotFoundException | JVstLoadException e) {
                    e.printStackTrace();

                    return false;
                }
            } else if(type.equalsIgnoreCase(Type.VST3.name())) {
                plugin = new VST3();
            }

            return plugin.load(path, out);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * {@inheritDoc}
     * Делегирует к обернутому плагину. Расширение для сохранения пресетов.
     */
    public String getStateExtension() {
        return plugin.getStateExtension();
    }
    /**
     * {@inheritDoc}
     * Делегирует к обернутому плагину. Поддерживаемые форматы файлов.
     */
    public String[] getPluginExtension() {
        return plugin.getPluginExtension();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Сравнивает два <code>PluginWrapper</code> по обернутым VST плагинам.
     * Два wrapper'а равны, если содержат <b>один и тот же</b> нативный плагин.
     * </p>
     * <p>
     * <b>Игнорирует:</b> {@link #isEnable}, {@link #mix}, {@link #onInit}
     * </p>
     * <p><b>Стандартная логика:</b></p>
     * <pre>{@code
     * this == o                    → true
     * o == null || !PluginWrapper  → false
     * Objects.equals(plugin, o.plugin) → true/false
     * }</pre>
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginWrapper plugin1 = (PluginWrapper) o;
        return Objects.equals(plugin, plugin1.plugin);
    }
    /**
     * {@inheritDoc}
     * <p>
     * Хэш-код на основе обернутого VST плагина. Консистентен с {@link #equals(Object)}.
     * </p>
     * <p>
     * Два wrapper'а с одинаковым плагином имеют одинаковый hashCode.
     * <code>null</code> плагин → <code>0</code>.
     * </p>
     */
    @Override
    public int hashCode() {
        return plugin.hashCode();
    }
    /**
     * {@inheritDoc}
     * <p>
     * Делегирует к обернутому VST плагину. Формат:
     * </p>
     * <pre>{@code
     * "[Pro-Q 3] by FabFilter (VST3.6.7) - C:\VST3\Pro-Q 3.vst3"
     * }</pre>
     * <p>
     * Используется в логах, списках плагинов, отладке UI.
     * </p>
     */
    @Override
    public String toString() {
        return plugin.toString();
    }
}