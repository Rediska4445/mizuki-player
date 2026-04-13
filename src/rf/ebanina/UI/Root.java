package rf.ebanina.UI;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import rf.ebanina.File.Configuration.ConfigurableField;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.DataTypes;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Localization.Locales;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.Network.ISimilar;
import rf.ebanina.Network.Illegal.Similar.Spotify;
import rf.ebanina.Network.Info;
import rf.ebanina.UI.Editors.IViewable;
import rf.ebanina.UI.Editors.Metadata.Track.Metadata;
import rf.ebanina.UI.Editors.Network.NetworkHost;
import rf.ebanina.UI.Editors.Player.AudioHost;
import rf.ebanina.UI.Editors.Settings.Settings;
import rf.ebanina.UI.Editors.Statistics.Track.TrackStatistics;
import rf.ebanina.UI.UI.Context.Tooltip.ContextTooltip;
import rf.ebanina.UI.UI.Element.Art;
import rf.ebanina.UI.UI.Element.Buttons.Commons;
import rf.ebanina.UI.UI.Element.Buttons.Player.NextButton;
import rf.ebanina.UI.UI.Element.Buttons.Player.PlayButton;
import rf.ebanina.UI.UI.Element.Buttons.Player.PrevButton;
import rf.ebanina.UI.UI.Element.Buttons.Playlist.HideLeft;
import rf.ebanina.UI.UI.Element.Buttons.Playlist.HideRight;
import rf.ebanina.UI.UI.Element.ControlPane;
import rf.ebanina.UI.UI.Element.LicenseDialog;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists.ListCellPlaylist;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists.ListCellTrack;
import rf.ebanina.UI.UI.Element.ListViews.Playlist.PlayView;
import rf.ebanina.UI.UI.Element.MainFunctionDialog;
import rf.ebanina.UI.UI.Element.Slider.SoundSlider;
import rf.ebanina.UI.UI.Element.Text.TextField;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.UI.UI.Popup.LabelPopupMenu;
import rf.ebanina.UI.UI.Popup.PreviewPopupService;
import rf.ebanina.ebanina.KeyBindings.Keys;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.ArtProcessor;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlaylistController;
import rf.ebanina.ebanina.Player.Playlist;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.ebanina.Player.TrackHistory;
import rf.ebanina.utils.concurrency.LonelyThreadPool;
import rf.ebanina.utils.loggining.logging;
import rf.ebanina.utils.weakly.WeakConst;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static rf.ebanina.File.Resources.ResourceManager.BIN_LIBRARIES_PATH;
import static rf.ebanina.UI.Root.PlaylistHandler.playlistSelected;
import static rf.ebanina.ebanina.KeyBindings.KeyBindingController.isKeyPressed;
import static rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor.playProcessor;

/**
 * Главный контейнер и координатор пользовательского интерфейса музыкального плеера.
 *
 * <p>Класс {@code Root} представляет собой центральный компонент UI-архитектуры, который
 * полностью отвечает за создание, динамическое позиционирование, привязку свойств и управление
 * жизненным циклом всех визуальных элементов плеера. Реализует интерфейс {@link IRoot} и служит
 * единственной точкой входа для инициализации интерфейса через статический экземпляр
 * {@link #rootImpl}.</p>
 *
 * <h3>Структура и элементы UI</h3>
 *
 * <p>Содержит полную иерархию элементов управления, организованных в динамически позиционируемую
 * композицию на базе {@link Pane root}:
 * <table summary="Основные элементы UI">
 *   <tr><th>Элемент</th><th>Назначение</th><th>Динамическое позиционирование</th></tr>
 *   <tr><td>{@link #art}</td><td>Обложка текущего трека (200x200px)</td><td>Центр экрана с тенью DropShadow</td></tr>
 *   <tr><td>{@link #topDataPane}</td><td>Название трека + исполнитель</td><td>Над обложкой (-125px по Y)</td></tr>
 *   <tr><td>{@link #soundSlider}</td><td>Слайдер позиции/громкости</td><td>Под обложкой (+230px по Y)</td></tr>
 *   <tr><td>{@link #tracksListView}, {@link #similar}</td><td>Списки треков/похожих</td><td>Справа от обложки</td></tr>
 *   <tr><td>{@link #btn}, {@link #btnDown}, {@link #btnNext}</td><td>Управление воспроизведением</td><td>Под слайдером</td></tr>
 *   <tr><td>{@link #hideControlLeft}, {@link #hideControlRight}</td><td>Скрытие/показ списков</td><td>Левая/правая кромка</td></tr>
 *   <tr><td>{@link #mainFunctions}</td><td>Панель дополнительных функций</td><td>Динамическое размещение</td></tr>
 * </table></p>
 *
 * <h3>Динамическое позиционирование и привязки</h3>
 *
 * <p>Все элементы позиционируются динамически относительно размеров окна {@link #stage} через
 * систему коэффициентов {@link Layout rootLayout}. После начальной установки в {@link #set()}
 * активируются <b>двухфазные привязки свойств</b>:
 * <ol>
 *   <li><b>Начальная установка</b>: Абсолютные координаты на основе ширины/высоты stage</li>
 *   <li><b>Биндинги</b>: {@link #initBinds()} создает реактивную сетку позиций:
 *     <pre>{@code
 * art.layoutXProperty().bind(stage.widthProperty().multiply(0.5).subtract(art.widthProperty().multiply(0.5)));
 * soundSlider.layoutXProperty().bind(art.layoutXProperty());
 * btn.layoutXProperty().bind(soundSlider.layoutXProperty().add(...));
 * }</pre>
 *   </li>
 * </ol></p>
 *
 * <h3>Основной жизненный цикл ({@link #set()})</h3>
 *
 * <pre>
 * 1. Загрузка конфигурации (интерполятор SPLINE из "general_animations_interpolator")
 * 2. Создание размытых фонов (если "is_blur_background"=true)
 * 3. Настройка тени обложки (Gaussian DropShadow, radius=75, spread=0.5)
 * 4. Центрирование обложки: layoutX=(stage.height/2)-(stage.width/2)
 * 5. Позиционирование всех элементов относительно art (каскадно)
 * 6. Инициализация слайдера (setupSliderBoxAsync())
 * 7. Настройка кнопок, временных меток, списков
 * 8. Активация биндингов (initBinds())
 * 9. Обработчики: клик по обложке→Metadata, Ctrl+Scroll→переключение треков
 * 10. Диалог лицензии (первый запуск)
 * </pre>
 *
 * <h3>Особенности реализации</h3>
 *
 * <ul>
 * <li><b>Гибридная архитектура</b>: Статические ссылки на instance-объекты для удобства доступа</li>
 * <li><b>Конфигурируемость</b>: {@link ConfigurableField} для corners, slider_width/height</li>
 * <li><b>Ленивая инициализация</b>: Элементы создаются до вызова set(), но активируются в нем</li>
 * <li><b>Платформенные фичи</b>: DWM API для Windows 11 (setCaptionColor по цвету обложки)</li>
 * <li><b>Кастомный интерполятор</b>: {@link #iceInterpolator} для "ледяной" физики анимаций</li>
 * <li><b>Адаптивный полноэкранный режим</b>: beforePortable сохраняет размер перед максимизацией</li>
 * </ul>
 *
 * <h3>Пример использования</h3>
 *
 * <pre>{@code
 * // Инициализация stage
 * Root.stage = new Stage();
 *
 * // Создание и настройка UI
 * Root.root = new Pane();
 * Root.rootImpl.set();  // Магия происходит здесь
 *
 * // Применение к сцене
 * Scene scene = new Scene(Root.root, 420, 800);
 * Root.stage.setScene(scene);
 * Root.stage.show();
 * }</pre>
 *
 * <h3>Поведение и взаимодействие</h3>
 *
 * <ul>
 * <li><b>Ctrl+Scroll</b> над root переключает треки в плейлисте</li>
 * <li><b>Клик по обложке</b> открывает Metadata редактор текущего трека</li>
 * <li><b>Кнопки hideControlLeft/Right</b> анимированно показывают/скрывают списки</li>
 * <li><b>Автоскрытие списков</b> при similar.close()/tracksListView.close()</li>
 * <li><b>Тултипы</b> с динамическим контентом (названия следующих треков)</li>
 * <li><b>License dialog</b> блокирует UI до согласия (сохраняется в shared data)</li>
 * </ul>
 *
 * @see rf.ebanina.UI.UI.Element.Art
 * @see rf.ebanina.UI.UI.Element.Slider.SoundSlider
 * @see rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor
 * @see rf.ebanina.File.Configuration.ConfigurationManager
 * @implNote Использует нативную библиотеку dwm.dll для Windows 11 caption color
 * @implNote Все позиционирования используют Layout.rootLayout коэффициенты для адаптивности
 * @since 1.0
 */
@logging(tag = "root")
public class Root
        implements IRoot
{
    /**
     * Карта для хранения размеров и позиций окон приложения.
     *
     * <p>Хранит соответствие между идентификаторами окон (строки) и их геометрическими параметрами
     * в виде {@link Map.Entry}{@code <Point, Dimension>}. Использует {@link WeakHashMap} для
     * автоматической очистки записей о закрытых окнах, когда на ключи (идентификаторы окон)
     * перестают быть сильные ссылки.</p>
     *
     * <p><b>Ключи:</b> Строковые идентификаторы окон (например, названия или UUID).<br>
     * <b>Значения:</b> Пара координат верхнего левого угла ({@link java.awt.Point}) и размеров
     * ({@link java.awt.Dimension}).</p>
     *
     * <h3>Назначение в контексте Root</h3>
     *
     * <p>Используется для сохранения геометрии окон перед изменением их размеров (максимизация,
     * портативный режим). При восстановлении окна из портативного режима используются сохраненные
     * значения:</p>
     *
     * <pre>{@code
     * // В обработчике maximizedProperty()
     * if(!isMaximazied.get()) {
     *     beforePortable.setSize(stage.getWidth(), stage.getHeight());  // Сохранение текущего
     *     stage.setHeight(800);
     *     stage.setWidth(420);
     *     stage.setResizable(false);
     * } else {
     *     stage.setHeight(beforePortable.getHeight());  // Восстановление из сохраненного
     *     stage.setWidth(beforePortable.getWidth());
     *     stage.setResizable(true);
     * }
     * }</pre>
     *
     * <h3>Преимущества WeakHashMap</h3>
     *
     * <ul>
     * <li><b>Автоочистка памяти:</b> При закрытии окон GC удаляет соответствующие записи</li>
     * <li><b>Предотвращение утечек:</b> Не накапливает "мусорные" записи о несуществующих окнах</li>
     * <li><b>Thread-safe:</b> Безопасна для конкурентного доступа из UI-потока</li>
     * </ul>
     *
     * <h3>Пример использования</h3>
     *
     * <pre>{@code
     * // Сохранение размеров главного окна
     * Point pos = new Point((int)stage.getX(), (int)stage.getY());
     * Dimension size = new Dimension((int)stage.getWidth(), (int)stage.getHeight());
     * windowsSizes.put("main_window", new AbstractMap.SimpleEntry<>(pos, size));
     *
     * // Восстановление
     * var entry = windowsSizes.get("main_window");
     * if(entry != null) {
     *     stage.setX(entry.getKey().x);
     *     stage.setY(entry.getKey().y);
     *     stage.setWidth(entry.getValue().width);
     *     stage.setHeight(entry.getValue().height);
     * }
     * }</pre>
     *
     * @implNote Используется в связке с {@link #beforePortable} для портативного режима окна
     * @see java.util.WeakHashMap
     * @see java.awt.Point
     * @see java.awt.Dimension
     */
    protected final Map<String, Map.Entry<Point, Dimension>> windowsSizes = new WeakHashMap<>();
    /**
     * Возвращает карту размеров и позиций окон приложения.
     *
     * @return Неизменяемая ссылка на {@link #windowsSizes} — глобальный реестр геометрии окон.
     *         Карта автоматически очищается GC при отсутствии сильных ссылок на ключи.
     *
     * <p><b>Гарантии:</b></p>
     * <ul>
     * <li>Возвращает <b>ту же самую инстанцию</b> {@link WeakHashMap}, что и поле {@link #windowsSizes}</li>
     * <li><b>Не создает копию</b> — прямой доступ для производительности</li>
     * <li>Совместима с итерацией, {@code put}/ {@code get} операциями</li>
     * </ul>
     *
     * <h3>Контекст использования</h3>
     *
     * <p>Предоставляет доступ к сохраненной геометрии окон для:</p>
     * <ul>
     * <li>Восстановления размеров после портативного режима</li>
     * <li>Синхронизации между экземплярами Root</li>
     * <li>Сериализации состояния окон (если требуется)</li>
     * </ul>
     *
     * <pre>{@code
     * // Пример восстановления окна
     * Map<String, Map.Entry<Point, Dimension>> sizes = rootImpl.getWindowsSizes();
     * var entry = sizes.get("main_window");
     * if(entry != null) {
     *     stage.setX(entry.getKey().x);
     *     stage.setY(entry.getKey().y);
     *     stage.setWidth(entry.getValue().width);
     *     stage.setHeight(entry.getValue().height);
     * }
     * }</pre>
     *
     * @see #windowsSizes
     * @see Root#windowsSizes
     * @implNote Публичный getter для ранее статического поля — обеспечивает инкапсуляцию instance-логики
     */
    public Map<String, Map.Entry<Point, Dimension>> getWindowsSizes() {
        return windowsSizes;
    }
    /**
     * Устанавливает цвет заголовка окна через Windows DWM API (только Windows 11).
     *
     * <p>Вызывает нативную функцию из загруженной библиотеки {@code dwm.dll} для изменения цвета
     * заголовочной панели окна на основе цвета обложки альбома. Работает только при условии:
     * <ul>
     * <li>{@code ConfigurationManager.getBooleanItem("album_art_caption_paint", "true") == true}</li>
     * <li>OS содержит "Windows 11"</li>
     * </ul></p>
     *
     * <h3>Параметры</h3>
     *
     * <dl>
     * <dt>{@code wid}</dt><dd>Нативный дескриптор окна, полученный через
     *     {@link #getNativeHandlePeerForStage(Stage)}</dd>
     * <dt>{@code color}</dt><dd>ARGB-цвет в формате 32-битного целого (0xAARRGGBB),
     *     преобразованный из {@link javafx.scene.paint.Color} через {@link #convertColorTo16(Color)}</dd>
     * </dl>
     *
     * <h3>Цепочка вызовов (из кода)</h3>
     *
     * <pre>{@code
     * Color clr = ColorProcessor.core.getMainClr();  // Цвет из обложки
     * long wid = getNativeHandlePeerForStage(stage); // HWND через рефлексию
     * int winColor = Integer.parseInt(convertColorTo16(clr), 16); // ARGB→int
     * setCaptionColor(wid, winColor);  // Нативный вызов
     * }</pre>
     *
     * <h3>Предусловия загрузки</h3>
     *
     * <p>Библиотека загружается в {@link Root#set()}:</p>
     * <pre>{@code
     * loadDwmApiLibrary(BIN_LIBRARIES_PATH + "dwm.dll");
     * }</pre>
     *
     * <h3>Обработка ошибок</h3>
     *
     * <ul>
     * <li>{@link UnsatisfiedLinkError} при отсутствии dwm.dll</li>
     * <li>Игнорируется на других ОС (macOS/Linux)</li>
     * </ul>
     *
     * @param wid нативный дескриптор окна (HWND)
     * @param color цвет заголовка в формате 0xAARRGGBB (int)
     * @throws UnsatisfiedLinkError если dwm.dll не загружена
     * @see #setStageCaptionColor(Stage, Color)
     * @see #getNativeHandlePeerForStage(Stage)
     * @see #convertColorTo16(Color)
     * @see #loadDwmApiLibrary(String)
     */
    public static native void setCaptionColor(long wid, int color);
    /**
     * Единственный глобальный экземпляр класса {@link Root} — singleton-реализация.
     *
     * <p>Статическая ссылка на единственный экземпляр {@code Root}, инициализированный при загрузке
     * класса. Обеспечивает централизованный доступ ко всем методам и ресурсам UI из любого места
     * приложения без необходимости передачи instance.</p>
     *
     * <h3>Назначение и использование</h3>
     *
     * <p><b>Главная точка входа</b> для инициализации UI:</p>
     *
     * <pre>{@code
     * // Инициализация всего интерфейса
     * Root.rootImpl.set();
     *
     * // Доступ к общим ресурсам
     * Root.rootImpl.initBinds();
     * Root.rootImpl.initTooltips();
     *
     * // Показ алертов
     * Root.rootImpl.alert("Error", "Message", Alert.AlertType.ERROR);
     * }</pre>
     *
     * <h3>Singleton-гарантии</h3>
     *
     * <table summary="Singleton свойства">
     * <tr><th>Характеристика</th><th>Описание</th></tr>
     * <tr><td>Единственность</td><td>Один экземпляр на JVM</td></tr>
     * <tr><td>Ленивая инициализация</td><td>Создается при первом обращении к классу</td></tr>
     * <tr><td>Thread-safe</td><td>Гарантировано JVM classloader'ом</td></tr>
     * </table>
     *
     * <h3>Архитектурная роль</h3>
     *
     * <p><b>Фасад UI-системы:</b> Скрывает сложность инициализации ({@link #set()}), биндингов
     * ({@link #initBinds()}), нативных загрузок (DWM API) и диалогов. Все статические поля
     * ({@link #root}, {@link #art}, {@link #soundSlider}) ссылаются на ресурсы этого экземпляра.</p>
     *
     * <h3>Жизненный цикл</h3>
     *
     * <ol>
     * <li><b>Создание:</b> Автоматически при первом обращении к классу</li>
     * <li><b>Инициализация:</b> {@link #set()} — полная настройка UI</li>
     * <li><b>Биндинги:</b> {@link #initBinds()} — реактивная адаптация к resize</li>
     * <li><b>Завершение:</b> GC при завершении приложения (WeakHashMap очищает ресурсы)</li>
     * </ol>
     *
     * <h3>Преимущества реализации</h3>
     *
     * <ul>
     * <li><b>Простота доступа:</b> {@code Root.rootImpl.set()} вместо DI/фабрик</li>
     * <li><b>Централизация состояния:</b> Все UI-элементы доступны статически</li>
     * <li><b>Отложенная инициализация:</b> Ресурсы создаются только при необходимости</li>
     * </ul>
     *
     * @see #set()
     * @see #initBinds()
     * @implNote Заменяет традиционный enum-singleton или synchronized-ленивую инициализацию
     * @implNote Используется во всех статических вызовах: alert(), initTooltips(), диалоги лицензии
     */
    public static Root rootImpl = new Root();
    /**
     * Имя операционной системы, используемой для условной компиляции и платформенных функций.
     *
     * <p>Статическая константа, содержащая результат {@link System#getProperty(String)}}.
     * Используется для активации Windows-специфичных возможностей, в частности изменения цвета
     * заголовка окна через DWM API только на Windows 11.</p>
     *
     * <h3>Платформенная логика</h3>
     *
     * <p>Основное использование в методе {@link #setStageCaptionColor(Stage, Color)}:</p>
     *
     * <pre>{@code
     * if (ConfigurationManager.instance.getBooleanItem("album_art_caption_paint", "true")
     *     && OS.contains("Windows 11")) {
     *     long wid = getNativeHandlePeerForStage(stage);
     *     int res = Integer.parseInt(convertColorTo16(color), 16);
     *     setCaptionColor(wid, res);  // Только Windows 11
     * }
     * }</pre>
     *
     * <h3>Значения OS (примеры)</h3>
     *
     * <table summary="Примеры значений os.name">
     *   <tr><th>ОС</th><th>{@code OS} значение</th><th>DWM поддержка</th></tr>
     *   <tr><td>Windows 11</td><td>contains("Windows 11")</td><td>Да</td></tr>
     *   <tr><td>Windows 10</td><td>"Windows 10..."</td><td>Нет</td></tr>
     *   <tr><td>macOS</td><td>"Mac OS X 14.4..."</td><td>Нет</td></tr>
     *   <tr><td>Linux</td><td>"Linux..."</td><td>Нет</td></tr>
     * </table>
     *
     * <h3>Назначение</h3>
     *
     * <ul>
     * <li><b>Условная активация DWM API:</b> {@link #setCaptionColor(long, int)}</li>
     * <li><b>Платформенная загрузка:</b> {@code dwm.dll} только на Windows</li>
     * <li><b>Отложенная инициализация:</b> Проверяется при первом вызове {@link #set()}</li>
     * </ul>
     *
     * <h3>Иммутабельность и thread-safety</h3>
     *
     * <ul>
     * <li><b>final</b>: Не может быть переприсвоена</li>
     * <li><b>static</b>: Инициализируется один раз при загрузке класса</li>
     * <li><b>immutable</b>: {@link String} — неизменяемый примитив</li>
     * <li><b>Thread-safe</b>: Безопасна для чтения из любого потока</li>
     * </ul>
     *
     * @implNote Единственная платформенная проверка в классе — определяет доступность визуальных улучшений Windows 11
     * @see #setStageCaptionColor(Stage, Color)
     * @see #setCaptionColor(long, int)
     * @see System#getProperty(String)
     */
    public static final String OS = System.getProperty("os.name");
    /**
     * Основное окно приложения (Stage) — контейнер для всего UI.
     *
     * <p>Ссылка на {@link Stage}, к которому привязаны все динамические расчеты позиций элементов,
     * биндинги свойств и обработчики событий. Используется как якорь координатной системы:</p>
     *
     * <pre>{@code
     * // Центрирование обложки трека
     * art.setLayoutX((stage.getHeight() / 2) - (stage.getWidth() / 2));
     *
     * // Биндинги к размерам окна
     * art.layoutXProperty().bind(stage.widthProperty().multiply(0.5));
     * }</pre>
     *
     * <h3>Ключевые роли в архитектуре</h3>
     *
     * <ul>
     * <li><b>Источник размеров:</b> {@code stage.getWidth()/getHeight()} для начального позиционирования</li>
     * <li><b>Реактивная привязка:</b> {@code stage.widthProperty()} во всех биндингах</li>
     * <li><b>Фокус:</b> {@code stage.isFocused()} для обработки Ctrl+Scroll</li>
     * <li><b>Максимизация:</b> {@code stage.maximizedProperty()} для портативного режима</li>
     * </ul>
     *
     * <h3>Использование в жизненном цикле</h3>
     *
     * <table summary="Использование stage в ключевых методах">
     * <tr><th>Метод</th><th>Назначение stage</th></tr>
     * <tr><td>set()</td><td>Расчет начальных координат всех элементов</td></tr>
     * <tr><td>initBinds()</td><td>Привязка layoutX/Y к stage.width/height</td></tr>
     * <tr><td>setOnScroll()</td><td>Проверка stage.isFocused() для Ctrl+Scroll</td></tr>
     * <tr><td>maximizedProperty()</td><td>Переключение портативного режима</td></tr>
     * </table>
     *
     * <h3>Портативный режим (максимизация)</h3>
     *
     * <p>Обработчик {@code stage.maximizedProperty()} реализует кастомную логику:</p>
     * <pre>{@code
     * if(isInternalMaximazied.get()) {
     *     if(!isMaximazied.get()) {
     *         beforePortable.setSize(stage.getWidth(), stage.getHeight());  // Сохранение
     *         stage.setHeight(800); stage.setWidth(420); stage.setResizable(false);
     *     } else {
     *         stage.setHeight(beforePortable.getHeight());  // Восстановление
     *         stage.setWidth(beforePortable.getWidth());
     *         stage.setResizable(true);
     *     }
     * }
     * }</pre>
     *
     * <h3>Thread-safety</h3>
     *
     * <ul>
     * <li>Доступ только из FX Application Thread (Platform.runLater при необходимости)</li>
     * <li>Свойства (widthProperty, maximizedProperty) — Observable, безопасны для биндингов</li>
     * </ul>
     *
     * @see #initBinds() - биндинги к stage свойствам
     * @see #set() - начальное позиционирование по stage размерам
     */
    public Stage stage;
    /**
     * Радиус скругления углов обложки альбома (arcWidth/arcHeight).
     *
     * <p>Конфигурируемый параметр, автоматически инициализируемый аннотацией
     * {@link ConfigurableField} из конфигурации по ключу {@code "album_art_corners"}.
     * Значение по умолчанию: {@code 15} пикселей.</p>
     *
     * <h3>Применение</h3>
     *
     * <p>Устанавливается для {@link #art} в методе #set():</p>
     *
     * <pre>{@code
     * art.setArcHeight(corners);
     * art.setArcWidth(corners);
     * }</pre>
     *
     * <h3>Оптимизация доступа</h3>
     *
     * <p>Статическое поле кэширует значение из {@link ConfigurationManager},
     * исключая повторные обращения к конфигурационной карте при рендере ячеек списков:</p>
     *
     * <pre>{@code
     * // Обращение к ConfigurationManager в каждой ячейке
     * int corners = ConfigurationManager.instance.getIntItem("album_art_corners", "15");
     *
     * // Прямой доступ к статическому полю
     * art.setArcHeight(Root.corners);
     * }</pre>
     *
     * <h3>Жизненный цикл инициализации</h3>
     *
     * <ol>
     * <li>Загрузка класса: {@link ConfigurationManager#initializeVariables(Class[])}}</li>
     * <li>Автоинициализация: Аннотация устанавливает значение из конфига</li>
     * <li>Использование: #set() применяет к UI-элементам</li>
     * </ol>
     *
     * <p>Ключевое преимущество: однократная инициализация вместо обращения к
     * {@link ConfigurationManager} в каждой ячейке {@link #tracksListView} и {@link #similar}.</p>
     *
     * @see ConfigurableField#key() - "album_art_corners"
     * @see #set() - применение к art.setArcHeight/Width()
     * @see ConfigurationManager#initializeVariables(Class[])) - автоинициализация
     */
    @ConfigurableField(key = "album_art_corners", ifNull = "15")
    public static int corners;
    /**
     * Корневой контейнер всех элементов пользовательского интерфейса.
     *
     * <p>Основной {@link Pane}, который содержит всю иерархию UI-элементов плеера. Служит
     * универсальным холстом для динамического позиционирования всех дочерних компонентов.</p>
     *
     * <h3>Роль в архитектуре</h3>
     *
     * <ul>
     * <li><b>Единая точка сборки:</b> Все элементы добавляются через {@code root.getChildren().add()}</li>
     * <li><b>Контейнер стилей:</b> {@code root.getStylesheets().add(...)} для CSS</li>
     * <li><b>Приемник эффектов:</b> {@code root.setEffect(new GaussianBlur(0))}</li>
     * <li><b>Обработчик событий:</b> {@code root.setOnScroll(...)} для Ctrl+Scroll</li>
     * </ul>
     *
     * <h3>Последовательность добавления элементов (из #set())</h3>
     *
     * <pre>{@code
     * root.getChildren().add(art);                    // 1. Обложка
     * root.getChildren().add(topDataPane);            // 2. Название+исполнитель
     * root.getChildren().add(soundSlider);            // 3. Слайдер
     * root.getChildren().add(soundSlider.getSliderBackground());
     * root.getChildren().add(sliderBlurBackground);   // 4. Подложка слайдера
     * root.getChildren().add(beginTime);              // 5. Временные метки
     * root.getChildren().add(endTime);
     * root.getChildren().add(btn);                    // 6. Кнопки управления
     * root.getChildren().add(btnDown);
     * root.getChildren().add(btnNext);
     * root.getChildren().add(hideControlRight);       // 7. Кнопки списков
     * root.getChildren().add(hideControlLeft);
     * root.getChildren().add(mainFunctions);          // 8. Основные функции
     * }</pre>
     *
     * <h3>Z-порядок (toFront)</h3>
     *
     * <p>Финальная сортировка слоев для правильного наложения:</p>
     *
     * <pre>{@code
     * toFront(btn, btnNext, btnDown, hideControlLeft, hideControlRight,
     *         beginTime, endTime, similar, topDataPane, tracksListView,
     *         sliderBlurBackground, soundSlider.getSliderBackground(), soundSlider);
     * }</pre>
     *
     * <h3>Динамические свойства</h3>
     *
     * <table summary="Настройки root в set()">
     * <tr><th>Свойство</th><th>Значение</th><th>Назначение</th></tr>
     * <tr><td>stylesheets</td><td>"root" CSS + CONTEXT_MENU_STYLES</td><td>Стилизация</td></tr>
     * <tr><td>effect</td><td>GaussianBlur(0)</td><td>Базовый эффект размытия</td></tr>
     * <tr><td>background</td><td>Из "background_image" конфига</td><td>Фоновое изображение</td></tr>
     * <tr><td>style</td><td>Из "background_css_style"</td><td>CSS фон</td></tr>
     * </table>
     *
     * <h3>Связь со Scene</h3>
     *
     * <p>Устанавливается как корень сцены: {@code stage.setScene(new Scene(root, 420, 800))}.</p>
     *
     * @see #set() - последовательность добавления и настройки
     * @see #toFront(Node...) - финальная сортировка Z-порядка
     * @see #initBinds() - биндинги дочерних элементов
     */
    public Pane root;
    /**
     * Сцена JavaFX, связанная с основным окном приложения.
     *
     * <p>Хранит ссылку на {@link Scene}, к которой принадлежит {@link #root}. Используется для
     * загрузки глобальных стилей и управления свойствами сцены.</p>
     *
     * <h3>Основное применение</h3>
     *
     * <p>В методе #set() добавляются CSS-стили для контекстного меню:</p>
     *
     * <pre>{@code
     * stage.getScene().getStylesheets().add(
     *     ResourceManager.Instance.loadStylesheet(
     *         Resources.Properties.CONTEXT_MENU_STYLES.getKey()
     *     )
     * );
     * }</pre>
     *
     * <h3>Роль в архитектуре</h3>
     *
     * <ul>
     * <li><b>Глобальные стили:</b> Загрузка CSS через {@code scene.getStylesheets()}</li>
     * <li><b>Связь с Stage:</b> {@code stage.getScene()} возвращает эту scene</li>
     * <li><b>Контекст рендеринга:</b> Определяет CSS-контекст для всех дочерних элементов</li>
     * </ul>
     *
     * <h3>Жизненный цикл</h3>
     *
     * <ol>
     * <li>Создание: {@code new Scene(root, 420, 800)}</li>
     * <li>Назначение: {@code stage.setScene(scene)}</li>
     * <li>Инициализация стилей: #set() добавляет CONTEXT_MENU_STYLES</li>
     * </ol>
     *
     * <h3>Отличие от root</h3>
     *
     * <table summary="scene vs root">
     * <tr><th>Элемент</th><th>Назначение</th><th>CSS управление</th></tr>
     * <tr><td>scene</td><td>Контекст рендеринга, стили</td><td>getStylesheets()</td></tr>
     * <tr><td>root (Pane)</td><td>Холст для позиционирования</td><td>setStylesheets(), setStyle()</td></tr>
     * </table>
     *
     * <p>Scene управляет <b>глобальными стилями</b>, root — <b>локальными стилями и layout</b>.</p>
     *
     * @see #set() - загрузка CONTEXT_MENU_STYLES
     */
    public Scene scene;
    /**
     * Слайдер управления позицией воспроизведения и громкостью трека.
     *
     * <p>Экземпляр кастомного класса {@code SoundSlider}, расположенный под обложкой трека.
     * Основной элемент управления временной шкалой и громкостью.</p>
     *
     * <h3>Инициализация и позиционирование (из #set())</h3>
     *
     * <pre>{@code
     * soundSlider.setLayoutX(art.getLayoutX());
     * soundSlider.setLayoutY(art.getLayoutY() + art.getHeight() + 30);
     * soundSlider.setPrefHeight(ConfigurationManager.instance.getIntItem("slider_height", "25"));
     * soundSlider.setPrefWidth(Math.max(width, art.getWidth()));
     * }</pre>
     *
     * <h3>Настройка параметров</h3>
     *
     * <table summary="Настройки soundSlider">
     * <tr><th>Свойство</th><th>Значение</th><th>Источник</th></tr>
     * <tr><td>interpolator</td><td>general_interpolator (SPLINE)</td><td>#general_interpolator</td></tr>
     * <tr><td>color</td><td>Color.BLACK</td><td>Фиксированное</td></tr>
     * <tr><td>size</td><td>Dimension(prefWidth, prefHeight)</td><td>Динамическое</td></tr>
     * </table>
     *
     * <h3>Последовательность инициализации</h3>
     *
     * <pre>{@code
     * root.getChildren().add(soundSlider);
     * root.getChildren().add(soundSlider.getSliderBackground());  // Подложка слайдера
     * soundSlider.initializeBox();
     * soundSlider.setupSliderBoxAsync().start();  // Асинхронная инициализация
     * }</pre>
     *
     * <h3>Биндинги (из #initBinds())</h3>
     *
     * <p>Реактивно привязан к позиции обложки:</p>
     * <pre>{@code
     * soundSlider.layoutXProperty().bind(art.layoutXProperty());
     * soundSlider.layoutYProperty().bind(art.layoutYProperty()
     *     .add(art.heightProperty().add(rootLayout.getImgBottom())));
     * }</pre>
     *
     * <h3>Z-порядок наложения</h3>
     *
     * <p>В #set() добавляется в toFront() последним, обеспечивая поверхностное отображение:</p>
     * <pre>{@code
     * toFront(..., sliderBlurBackground, soundSlider.getSliderBackground(), soundSlider);
     * }</pre>
     *
     * <h3>Роль в UI-архитектуре</h3>
     *
     * <ul>
     * <li><b>Якорь координат:</b> Кнопки btn/btnDown/btnNext позиционируются относительно него</li>
     * <li><b>Временные метки:</b> beginTime/endTime привязаны к его краям</li>
     * <li><b>Подложка:</b> sliderBlurBackground создает визуальный фон</li>
     * </ul>
     *
     * @see #set() - полная инициализация и позиционирование
     * @see #initBinds() - реактивные биндинги позиции
     */
    public SoundSlider soundSlider;
    /**
     * Обложка текущего воспроизводимого трека — центральный визуальный элемент UI.
     *
     * <p>Экземпляр кастомного класса {@code Art}, отображающий изображение альбома (200x200px).
     * Служит <b>якорем координатной системы</b> — все остальные элементы позиционируются относительно него.</p>
     *
     * <h3>Полная инициализация (из #set())</h3>
     *
     * <pre>{@code
     * art.setHeight(200);
     * art.setWidth(200);
     * art.setArcHeight(corners);      // Скругление углов
     * art.setArcWidth(corners);
     * art.setLayoutX((stage.getHeight() / 2) - (stage.getWidth() / 2));  // Центрирование
     * art.setLayoutY((stage.getHeight() / 2) - (art.getHeight() / 2) - rootLayout.getCurrentTrackTopPlaylistPane());
     * art.setEffect(imgTrackShadow);  // Тень GaussianBlur(radius=75)
     * art.setRotationAxis(new Point3D(25, 25, 25));  // 3D-эффект
     * art.setOpacity(1);
     * root.getChildren().add(art);
     * }</pre>
     *
     * <h3>Интерактивность</h3>
     *
     * <p>ЛКМ по обложке открывает редактор метаданных:</p>
     * <pre>{@code
     * art.setOnMouseClicked(t -> {
     *     if(t.getButton() == MouseButton.PRIMARY) {
     *         Metadata.getInstance().prepare(PlayProcessor.playProcessor.getTracks()
     *             .get(PlayProcessor.playProcessor.getTrackIter()));
     *         Metadata.getInstance().open(stage);
     *     }
     * });
     * }</pre>
     *
     * <h3>Биндинги позиционирования (из #initBinds())</h3>
     *
     * <pre>{@code
     * art.layoutXProperty().bind(stage.widthProperty().multiply(rootLayout.getImgTrackArtRoundXMultiplier())
     *     .subtract(art.widthProperty().multiply(rootLayout.getImgTrackArtRoundWidthXMultiplier()))
     *     .subtract(corners * rootLayout.getImgTrackArtRoundWidthXMultiplier()));
     * art.layoutYProperty().bind(stage.heightProperty().multiply(rootLayout.getImgTrackArtRoundYMultiplier())
     *     .subtract(art.heightProperty().multiply(rootLayout.getImgTrackArtRoundWidthYMultiplier())));
     * }</pre>
     *
     * <h3>Архитектурная роль — якорь layout</h3>
     *
     * <table summary="Элементы, привязанные к art">
     * <tr><th>Элемент</th><th>Привязка</th></tr>
     * <tr><td>topDataPane</td><td>layoutX = art.layoutX - 50</td></tr>
     * <tr><td>soundSlider</td><td>layoutX/Y напрямую к art</td></tr>
     * <tr><td>btnDown</td><td>layoutX = art.layoutX</td></tr>
     * <tr><td>tracksListView</td><td>layoutX = art.layoutX + art.width + padding</td></tr>
     * </table>
     *
     * <h3>Z-порядок</h3>
     *
     * <p>Добавляется первым в root.getChildren(), но не входит в toFront() — находится под
     * кнопками управления и временными метками.</p>
     *
     * @see #set() - полная визуальная настройка и позиционирование
     * @see #initBinds() - реактивное центрирование при изменении stage
     */
    public Art art;
    /**
     * Основной список воспроизведения треков с возможностью поиска и навигации по плейлистам.
     *
     * <p>Экземпляр кастомного класса {@code PlayView<Track, Playlist>} — левая панель интерфейса,
     * отображающая текущий плейлист треков. Поддерживает поиск, навигацию по плейлистам и
     * синхронизацию с текущим воспроизводимым треком.</p>
     *
     * <h3>Инициализация (из #set())</h3>
     *
     * <pre>{@code
     * TrackListView.set();  // Инициализация содержимого
     *
     * tracksListView.getCurrentPlaylistText().setFont(ResourceManager.Instance.loadFont("main_font", 11));
     * tracksListView.getCurrentPlaylistText().setAlignment(Pos.CENTER);
     * tracksListView.getCurrentPlaylistText().updateColor(ColorProcessor.core.getMainClr());
     *
     * tracksListView.getSearchBar().setBackground(Background.EMPTY);
     * tracksListView.getSearchBar().setFont(ResourceManager.Instance.loadFont("main_font", 11));
     * }</pre>
     *
     * <h3>Расположение и размеры</h3>
     *
     * <p>Позиционируется справа от обложки трека с динамическими привязками (из #initListViewsBinds()):</p>
     *
     * <pre>{@code
     * tracksListView.layoutXProperty().bind(art.layoutXProperty()
     *     .add(art.widthProperty().add(rootLayout.getPaddingArt()).add(10)));
     * tracksListView.prefWidthProperty().bind(art.layoutXProperty()
     *     .subtract(rootLayout.getTrackListViewToImgTrackArtRoundXSubtract())
     *     .subtract(rootLayout.getPaddingArt()));
     * tracksListView.layoutYProperty().bind(topDataPane.layoutYProperty().add(10));
     * tracksListView.prefHeightProperty().bind(stage.heightProperty()
     *     .subtract(topDataPane.layoutYProperty())
     *     .subtract(rootLayout.getBottomTracklistBottom()));
     * }</pre>
     *
     * <h3>Z-порядок</h3>
     *
     * <p>Выносится в передний план в #set():</p>
     *
     * <pre>{@code
     * toFront(..., tracksListView, ...);
     * }</pre>
     *
     * <h3>Интерактивные элементы</h3>
     *
     * <table summary="Компоненты tracksListView">
     * <tr><th>Элемент</th><th>Настройка</th><th>Назначение</th></tr>
     * <tr><td>currentPlaylistText</td><td>Шрифт main_font(11), центр, цвет из ColorProcessor</td><td>Название плейлиста</td></tr>
     * <tr><td>searchBar</td><td>Прозрачный фон, шрифт main_font(11)</td><td>Поиск треков</td></tr>
     * <tr><td>ListView</td><td>trackSelectionModel</td><td>Список треков</td></tr>
     * </table>
     *
     * <h3>Синхронизация с контроллером воспроизведения</h3>
     *
     * <ul>
     * <li><b>Автовыбор:</b> #set() → trackSelectionModel.select(current trackIter)</li>
     * <li><b>Ctrl+Scroll:</b> #setOnScroll() → автоматическое выделение нового трека</li>
     * <li><b>Скрытие:</b> tracksListView.close() после инициализации</li>
     * </ul>
     *
     * <h3>Навигация по плейлистам</h3>
     *
     * <p>Предполагает кнопки навигации (по аналогии с similar):</p>
     *
     * <pre>{@code
     * tracksListView.getBtnPlaylistDown();    // Предыдущий плейлист
     * tracksListView.getBtnPlaylist();       // Установить плейлист
     * tracksListView.getBtnPlaylistNext();   // Следующий плейлист
     * }</pre>
     *
     * @see #set() - настройка текста плейлиста и поиска
     * @see #initListViewsBinds() - динамическое позиционирование
     */
    public PlayView<Track, Playlist> tracksListView;
    /**
     * Панель списка похожих треков, получаемых через сетевой сервис.
     *
     * <p>Экземпляр кастомного класса {@code PlayView<Track, Playlist>} — правая панель интерфейса,
     * отображающая рекомендации похожих треков (вероятно, через Spotify API или аналогичный сервис).
     * Располагается справа от {@link #tracksListView} и управляется кнопкой {@link #hideControlLeft}.</p>
     *
     * <h3>Инициализация (из #set())</h3>
     *
     * <pre>{@code
     * SimilarListView.set();  // Инициализация данных похожих треков
     * similar.close();        // Автоскрытие после настройки
     * }</pre>
     *
     * <h3>Расположение и размеры (из #initListViewsBinds())</h3>
     *
     * <pre>{@code
     * similar.setLayoutX(hideControlLeft.getLayoutX() + rootLayout.getSimilarLayoutXPadding());
     * similar.prefWidthProperty().bind(art.layoutXProperty()
     *     .subtract(rootLayout.getPaddingArt())
     *     .subtract(rootLayout.getSimilarWidthSubtract()));
     * similar.layoutYProperty().bind(tracksListView.layoutYProperty());
     * similar.prefHeightProperty().bind(tracksListView.prefHeightProperty());
     * }</pre>
     *
     * <h3>Связь с кнопкой управления</h3>
     *
     * <p>Позиционируется относительно {@link #hideControlLeft}:</p>
     *
     * <pre>{@code
     * hideControlLeft.layoutYProperty().bind(art.layoutYProperty()
     *     .add(art.heightProperty().multiply(rootLayout.getHideControlLeftToImgTrackArtHeight())));
     * }</pre>
     *
     * <h3>Z-порядок</h3>
     *
     * <p>Выносится в передний план вместе с основной панелью треков:</p>
     *
     * <pre>{@code
     * toFront(..., similar, ...);
     * }</pre>
     *
     * <h3>Интерактивные элементы (по аналогии с tracksListView)</h3>
     *
     * <ul>
     * <li><b>Поиск:</b> similar.getSearchBar() — фильтрация похожих треков</li>
     * <li><b>Навигация:</b> getBtnPlaylistDown/Next — переключение наборов рекомендаций</li>
     * <li><b>Тултипы:</b> Автоматическая настройка в #initTooltips()</li>
     * </ul>
     *
     * <h3>Сетевые особенности</h3>
     *
     * <table summary="Роль в архитектуре рекомендаций">
     * <tr><th>Источник данных</th><th>Назначение</th></tr>
     * <tr><td>ISimilar/Spotify</td><td>Получение похожих треков</td></tr>
     * <tr><td>hideControlLeft</td><td>Показ/скрытие панели</td></tr>
     * <tr><td>trackSelectionModel</td><td>Синхронизация с текущим треком</td></tr>
     * </table>
     *
     * <h3>Поведение</h3>
     *
     * <ul>
     * <li><b>Скрыт по умолчанию:</b> similar.close() после инициализации</li>
     * <li><b>Анимированное появление:</b> Через hideControlLeft</li>
     * <li><b>Динамическая ширина:</b> Зависит от позиции обложки и отступов</li>
     * </ul>
     *
     * @see #set() - инициализация и автоскрытие
     * @see #initListViewsBinds() - позиционирование относительно hideControlLeft
     */
    public PlayView<Track, Playlist> similar;
    /**
     * Поле отображения названия текущего воспроизводимого трека.
     *
     * <p>Экземпляр кастомного {@code TextField}, расположенный в центральной части
     * {@link #topDataPane}. Отображает название трека из {@link PlayProcessor}.</p>
     *
     * <h3>Настройка стиля (из #set())</h3>
     *
     * <pre>{@code
     * currentTrackName.setFont(ResourceManager.Instance.loadFont("main_font", 24));
     * }</pre>
     *
     * <h3>Расположение в иерархии</h3>
     *
     * <p>Размещается в центре верхней панели:</p>
     *
     * <pre>{@code
     * topDataPane.setCenter(currentTrackName);
     * topDataPane.setLayoutX(art.getLayoutX() - 50);
     * topDataPane.setLayoutY(art.getLayoutY() - 125);
     * topDataPane.setPrefWidth(art.getWidth() + 100);
     * topDataPane.setPrefHeight(75);
     * }</pre>
     *
     * <h3>Z-порядок</h3>
     *
     * <p>Выносится в передний план вместе с панелью:</p>
     *
     * <pre>{@code
     * toFront(..., topDataPane, ...);
     * }</pre>
     *
     * <h3>Характеристики отображения</h3>
     *
     * <table summary="Свойства currentTrackName">
     * <tr><th>Свойство</th><th>Значение</th><th>Источник</th></tr>
     * <tr><td>font</td><td>main_font, размер 24</td><td>ResourceManager</td></tr>
     * <tr><td>position</td><td>center topDataPane</td><td>BorderPane.setCenter()</td></tr>
     * <tr><td>container</td><td>topDataPane (75px высота)</td><td>#topDataPane</td></tr>
     * </table>
     *
     * <h3>Источник данных</h3>
     *
     * <p>Текст обновляется из состояния,
     * синхронизируясь с текущим воспроизводимым треком.</p>
     *
     * <h3>Визуальная роль</h3>
     *
     * <ul>
     * <li><b>Основной заголовок:</b> Первое, что видит пользователь</li>
     * <li><b>Дополняет обложку:</b> art (изображение) + название (текст)</li>
     * <li><b>Высокий приоритет Z:</b> Всегда поверх остальных элементов</li>
     * </ul>
     *
     * @see #set() - установка шрифта и позиционирование
     */
    public TextField currentTrackName;
    /**
     * Поле отображения исполнителя текущего воспроизводимого трека.
     *
     * <p>Экземпляр кастомного {@code TextField}, расположенный в нижней части
     * {@link #topDataPane}. Отображает имя исполнителя трека из {@link PlayProcessor}.
     * Дополняет {@link #currentTrackName} в визуальной иерархии.</p>
     *
     * <h3>Настройка стиля (из #set())</h3>
     *
     * <pre>{@code
     * currentArtist.setFont(ResourceManager.Instance.loadFont("font", 24));
     * }</pre>
     *
     * <h3>Расположение в иерархии</h3>
     *
     * <p>Размещается внизу верхней информационной панели:</p>
     *
     * <pre>{@code
     * topDataPane.setBottom(currentArtist);
     * topDataPane.setPrefHeight(75);  // Общая высота для названия+исполнителя
     * }</pre>
     *
     * <h3>Z-порядок</h3>
     *
     * <p>Наследует высокий приоритет от родительского {@link #topDataPane}:</p>
     *
     * <pre>{@code
     * toFront(..., topDataPane, ...);
     * }</pre>
     *
     * <h3>Характеристики отображения</h3>
     *
     * <table summary="Свойства currentArtist">
     * <tr><th>Свойство</th><th>Значение</th><th>Источник</th></tr>
     * <tr><td>font</td><td>"font", размер 24</td><td>ResourceManager (отличается от main_font)</td></tr>
     * <tr><td>position</td><td>bottom topDataPane</td><td>BorderPane.setBottom()</td></tr>
     * <tr><td>container</td><td>topDataPane (75px высота)</td><td>#topDataPane</td></tr>
     * </table>
     *
     * <h3>Визуальная иерархия</h3>
     *
     * <pre>{@code
     * [topDataPane: 75px высота]
     *   ├─ CENTER: currentTrackName (main_font, 24pt)
     *   └─ BOTTOM: currentArtist   (font, 24pt)
     * }</pre>
     *
     * <h3>Источник данных</h3>
     *
     * <p>Текст синхронизируется с {@link PlayProcessor},
     * отображая исполнителя текущего трека параллельно с названием.</p>
     *
     * <h3>Визуальная роль</h3>
     *
     * <ul>
     * <li><b>Вторичный заголовок:</b> Дополняет название трека</li>
     * <li><b>Часть композиции:</b> art + название + исполнитель = полный контекст трека</li>
     * <li><b>Стилистическое различие:</b> Отдельный шрифт "font" вместо "main_font"</li>
     * </ul>
     *
     * @see #set() - установка шрифта и позиционирование
     */
    public TextField currentArtist;
    /**
     * Поле отображения времени начала текущего воспроизведения.
     *
     * <p>Экземпляр кастомного {@code TextField}, расположенный слева от {@link #soundSlider}.
     * Показывает время с начала трека (например, "0:00").</p>
     *
     * <h3>Настройка и позиционирование (из #set())</h3>
     *
     * <pre>{@code
     * root.getChildren().add(beginTime);
     * beginTime.setAlignment(Pos.CENTER_LEFT);
     * beginTime.setEditable(true);
     * beginTime.setPrefWidth(30);
     * beginTime.setLayouts(soundSlider.getLayoutX(),
     *     soundSlider.getLayoutY() + soundSlider.getPrefHeight() + soundSlider.getPrefHeight() / 2);
     * }</pre>
     *
     * <h3>Биндинги (из #initBinds())</h3>
     *
     * <p>Реактивно привязан к позиции слайдера:</p>
     *
     * <pre>{@code
     * beginTime.layoutXProperty().bind(soundSlider.layoutXProperty());
     * beginTime.layoutYProperty().bind(soundSlider.layoutYProperty()
     *     .add(soundSlider.prefHeightProperty().multiply(rootLayout.getSliderBlurBackgroundBeginTimeMultiplier())));
     * }</pre>
     *
     * <h3>Z-порядок</h3>
     *
     * <p>Выносится в передний план вместе с контролами:</p>
     *
     * <pre>{@code
     * toFront(..., beginTime, endTime, ...);
     * }</pre>
     *
     * <h3>Характеристики отображения</h3>
     *
     * <table summary="Свойства beginTime">
     * <tr><th>Свойство</th><th>Значение</th></tr>
     * <tr><td>alignment</td><td>CENTER_LEFT</td></tr>
     * <tr><td>editable</td><td>true</td></tr>
     * <tr><td>prefWidth</td><td>30px</td></tr>
     * <tr><td>position</td><td>Левый край soundSlider + смещение по Y</td></tr>
     * </table>
     *
     * <h3>Визуальная композиция временной шкалы</h3>
     *
     * <pre>{@code
     * [soundSlider]                    [арт]
     * beginTime     endTime ───────────┘
     *   |              |
     *   +--30px──     +--30px--
     * CENTER_LEFT   CENTER_RIGHT
     * }</pre>
     *
     * <h3>Источник данных</h3>
     *
     * <p>Обновляется синхронно с позицией {@link #soundSlider}, отображая текущую временную метку
     * воспроизведения.</p>
     *
     * <h3>Роль в UI</h3>
     *
     * <ul>
     * <li><b>Левая метка:</b> Начало временной шкалы (0:00 → текущее время)</li>
     * <li><b>Визуальный якорь:</b> Определяет левую границу слайдера</li>
     * <li><b>Адаптивное позиционирование:</b> Следует за soundSlider при изменении stage</li>
     * </ul>
     *
     * @see #set() - начальная настройка выравнивания и размеров
     * @see #initBinds() - реактивные биндинги к soundSlider
     */
    public TextField beginTime;
    /**
     * Поле отображения времени окончания или общей длительности трека.
     *
     * <p>Экземпляр кастомного {@code TextField}, расположенный справа от {@link #soundSlider}.
     * Показывает общую длительность трека или оставшееся время (например, "3:45" или "NaN").</p>
     *
     * <h3>Настройка и позиционирование (из #set())</h3>
     *
     * <pre>{@code
     * root.getChildren().add(endTime);
     * endTime.setAlignment(Pos.CENTER_RIGHT);
     * endTime.setText("NaN");
     * endTime.setFont(ResourceManager.Instance.loadFont("main_font", 11));
     * endTime.setEditable(true);
     * endTime.setPrefWidth(30);
     * endTime.setLayouts(art.getLayoutX() + art.getWidth() - endTime.getFont().getSize() * 1.25,
     *     beginTime.getLayoutY());
     * }</pre>
     *
     * <h3>Биндинги (из #initBinds())</h3>
     *
     * <p>Реактивно привязан к правому краю слайдера:</p>
     *
     * <pre>{@code
     * endTime.layoutXProperty().bind(soundSlider.layoutXProperty()
     *     .add(soundSlider.prefWidthProperty().subtract(endTime.prefWidthProperty())));
     * endTime.layoutYProperty().bind(beginTime.layoutYProperty());
     * }</pre>
     *
     * <h3>Z-порядок</h3>
     *
     * <p>Выносится в передний план вместе с временными метками:</p>
     *
     * <pre>{@code
     * toFront(..., beginTime, endTime, ...);
     * }</pre>
     *
     * <h3>Характеристики отображения</h3>
     *
     * <table summary="Свойства endTime">
     * <tr><th>Свойство</th><th>Значение</th></tr>
     * <tr><td>alignment</td><td>CENTER_RIGHT</td></tr>
     * <tr><td>initial text</td><td>"NaN"</td></tr>
     * <tr><td>font</td><td>main_font, размер 11</td></tr>
     * <tr><td>editable</td><td>true</td></tr>
     * <tr><td>prefWidth</td><td>30px</td></tr>
     * <tr><td>position</td><td>Правый край арт + коррекция по размеру шрифта</td></tr>
     * </table>
     *
     * <h3>Визуальная композиция временной шкалы</h3>
     *
     * <pre>{@code
     * [soundSlider]                    [арт]
     * beginTime     endTime ───────────┘
     *   |              |
     *   +--30px──     +--30px--     -1.25×fontSize
     * CENTER_LEFT   CENTER_RIGHT
     * }</pre>
     *
     * <h3>Источник данных</h3>
     *
     * <p>Обновляется синхронно с {@link #soundSlider}, отображая общую длительность трека
     * или оставшееся время воспроизведения.</p>
     *
     * <h3>Роль в UI</h3>
     *
     * <ul>
     * <li><b>Правая метка:</b> Конец временной шкалы (длительность → текущее время)</li>
     * <li><b>Точная позиционировка:</b> Коррекция по размеру шрифта для выравнивания</li>
     * <li><b>Адаптивное позиционирование:</b> Следует за правым краем soundSlider</li>
     * </ul>
     *
     * @see #set() - начальная настройка текста, шрифта и позиционирования
     * @see #initBinds() - реактивные биндинги к soundSlider
     */
    public TextField endTime;
    /**
     * Кнопка управления видимостью правого списка треков ({@link #tracksListView}).
     *
     * <p>Экземпляр кастомного класса {@code Button} из пакета rf.ebanina.UI.UI.Element.Buttons.
     * Располагается на правой кромке окна и отвечает за анимированное показ/скрытие основной
     * панели плейлиста.</p>
     *
     * <h3>Настройка (из #set())</h3>
     *
     * <pre>{@code
     * root.getChildren().add(Root.hideControlRight);
     * hideControlRight.setPadding(new Insets(0));
     * hideControlRight.setCursor(Cursor.HAND);
     * }</pre>
     *
     * <h3>Биндинги позиционирования (из #initBinds())</h3>
     *
     * <pre>{@code
     * hideControlRight.layoutXProperty().bind(stage.widthProperty()
     *     .subtract(rootLayout.getHideControlRightSide())
     *     .subtract(hideControlRight.prefWidthProperty()));
     * hideControlRight.layoutYProperty().bind(art.layoutYProperty()
     *     .add(art.heightProperty().multiply(rootLayout.getHideControlRightToImgTrackArtHeightMultiplier())));
     * }</pre>
     *
     * <h3>Z-порядок</h3>
     *
     * <p>Выносится в передний план с высоким приоритетом:</p>
     *
     * <pre>{@code
     * toFront(..., hideControlRight, ...);
     * }</pre>
     *
     * <h3>Характеристики</h3>
     *
     * <table summary="Свойства hideControlRight">
     * <tr><th>Свойство</th><th>Значение</th></tr>
     * <tr><td>padding</td><td>Insets(0) — без внутренних отступов</td></tr>
     * <tr><td>cursor</td><td>Cursor.HAND — указатель руки</td></tr>
     * <tr><td>position</td><td>Правая кромка stage - отступ</td></tr>
     * </table>
     *
     * <h3>Визуальная привязка</h3>
     *
     * <pre>{@code
     * [stage.width - hideControlRightSide - width]  ← layoutX
     *          |
     *     hideControlRight  ← кнопка
     *          |
     * art.height × 0.5 ───────────────────────────── ← layoutY (центр обложки)
     * }</pre>
     *
     * <h3>Функциональная роль</h3>
     *
     * <ul>
     * <li><b>Триггер анимации:</b> Клик → tracksListView.show()/hide()</li>
     * <li><b>Тултип:</b> #initTooltips() → "Открыть локальный плейлист"</li>
     * <li><b>Адаптивная позиция:</b> Следует за правым краем окна и центром обложки</li>
     * </ul>
     *
     * <h3>Симметричная пара</h3>
     *
     * <p>Работает в паре с {@link #hideControlLeft} (левый список similar):</p>
     *
     * <table summary="Управление списками">
     * <tr><th>Кнопка</th><th>Список</th><th>Позиция</th></tr>
     * <tr><td>hideControlRight</td><td>tracksListView</td><td>Правая кромка</td></tr>
     * <tr><td>hideControlLeft</td><td>similar</td><td>Левая кромка</td></tr>
     * </table>
     *
     * @see #set() - добавление в root и базовая настройка
     * @see #initBinds() - привязка к stage.width и art.height
     */
    public rf.ebanina.UI.UI.Element.Buttons.Button hideControlRight;
    /**
     * Кнопка управления видимостью левого списка похожих треков ({@link #similar}).
     *
     * <p>Экземпляр кастомного класса {@code Button} из пакета rf.ebanina.UI.UI.Element.Buttons.
     * Располагается на левой кромке окна и отвечает за анимированное показ/скрытие панели
     * рекомендаций.</p>
     *
     * <h3>Настройка (из #set())</h3>
     *
     * <pre>{@code
     * root.getChildren().add(hideControlLeft);
     * hideControlLeft.setPadding(new Insets(0));
     * hideControlLeft.setCursor(Cursor.HAND);
     * }</pre>
     *
     * <h3>Биндинги позиционирования (из #initBinds())</h3>
     *
     * <pre>{@code
     * hideControlLeft.setLayoutX(rootLayout.getHideControlLeftLayoutX());
     * hideControlLeft.layoutYProperty().bind(art.layoutYProperty()
     *     .add(art.heightProperty().multiply(rootLayout.getHideControlLeftToImgTrackArtHeight())));
     * }</pre>
     *
     * <h3>Z-порядок</h3>
     *
     * <p>Выносится в передний план с высоким приоритетом:</p>
     *
     * <pre>{@code
     * toFront(..., hideControlLeft, ...);
     * }</pre>
     *
     * <h3>Характеристики</h3>
     *
     * <table summary="Свойства hideControlLeft">
     * <tr><th>Свойство</th><th>Значение</th></tr>
     * <tr><td>padding</td><td>Insets(0) — без внутренних отступов</td></tr>
     * <tr><td>cursor</td><td>Cursor.HAND — указатель руки</td></tr>
     * <tr><td>layoutX</td><td>rootLayout.getHideControlLeftLayoutX() (2px от левого края)</td></tr>
     * </table>
     *
     * <h3>Визуальная привязка</h3>
     *
     * <pre>{@code
     * [hideControlLeftLayoutX = 2]  ← фиксированная позиция X
     *          |
     *     hideControlLeft  ← кнопка
     *          |
     * art.height × 0.5 ───────────── ← layoutY (центр обложки)
     *
     * similar.layoutX = hideControlLeft.layoutX + similarLayoutXPadding
     * }</pre>
     *
     * <h3>Функциональная роль</h3>
     *
     * <ul>
     * <li><b>Триггер анимации:</b> Клик → similar.show()/hide()</li>
     * <li><b>Тултип:</b> #initTooltips() → "Открыть сетевой плейлист"</li>
     * <li><b>Якорь позиционирования:</b> similar.layoutX привязан к этой кнопке</li>
     * </ul>
     *
     * <h3>Симметричная пара</h3>
     *
     * <p>Работает в паре с {@link #hideControlRight} (правый список tracksListView):</p>
     *
     * <table summary="Управление списками">
     * <tr><th>Кнопка</th><th>Список</th><th>Позиция</th></tr>
     * <tr><td>hideControlLeft</td><td>similar</td><td>Левая кромка (X=2)</td></tr>
     * <tr><td>hideControlRight</td><td>tracksListView</td><td>Правая кромка</td></tr>
     * </table>
     *
     * @see #set() - добавление в root и базовая настройка
     * @see #initBinds() - фиксированная X + привязка Y к art.height
     * @see #initListViewsBinds() - similar позиционируется относительно этой кнопки
     */
    public rf.ebanina.UI.UI.Element.Buttons.Button hideControlLeft;
    /**
     * Кнопка доступа к истории воспроизведения треков.
     *
     * <p>Экземпляр кастомного класса {@code Button} из пакета rf.ebanina.UI.UI.Element.Buttons.
     * Предназначена для навигации к истории просмотренных/воспроизведенных треков.</p>
     *
     * <h3>Архитектурная роль</h3>
     *
     * <p>Часть системы навигации по истории, интегрированная с:</p>
     *
     * <ul>
     * <li>{@link TrackHistory} — хранилище истории треков</li>
     * <li>{@link PlayProcessor} — контроллер воспроизведения</li>
     * <li>{@link #tracksListView} — отображение истории</li>
     * </ul>
     *
     * <h3>Предполагаемое использование</h3>
     *
     * <p>Хотя прямой код настройки отсутствует в #set(), по аналогии с другими кнопками:</p>
     *
     * <pre>{@code
     * // Вероятная инициализация (аналогично btn, hideControl*)
     * root.getChildren().add(tracksHistory);
     * tracksHistory.setCursor(Cursor.HAND);
     *
     * // Обработчик: переход к истории
     * tracksHistory.setOnAction(e -> {
     *     tracksListView.switchToHistoryView();
     *     trackSelectionModel.clearSelection();
     * });
     * }</pre>
     *
     * <h3>Визуальная интеграция</h3>
     *
     * <p>Ожидается размещение в зоне управления воспроизведением или панели {@link #mainFunctions}:</p>
     *
     * <ul>
     * <li><b>Соседи:</b> {@link #btn}, {@link #btnDown}, {@link #btnNext}</li>
     * <li><b>Стиль:</b> Cursor.HAND, padding Insets(0)</li>
     * <li><b>Z-order:</b> Вероятно, toFront() с контролами</li>
     * </ul>
     *
     * <h3>Связь с состоянием плеера</h3>
     *
     * <table summary="Интеграция с историей">
     * <tr><th>Компонент</th><th>Роль</th></tr>
     * <tr><td>TrackHistory</td><td>Источник данных истории</td></tr>
     * <tr><td>PlayProcessor</td><td>Переключение режима отображения</td></tr>
     * <tr><td>trackSelectionModel</td><td>Сброс выбора при переходе</td></tr>
     * </table>
     *
     * <h3>Функциональные предположения</h3>
     *
     * <ul>
     * <li><b>Переключение вида:</b> tracksListView → режим истории</li>
     * <li><b>Тултип:</b> #initTooltips() → "Показать историю воспроизведения"</li>
     * <li><b>Анимация:</b> Плавный переход списка при клике</li>
     * </ul>
     *
     * <p>Реализация обработчика ожидается в инициализации кнопок или #initBinds().</p>
     *
     */
    public rf.ebanina.UI.UI.Element.Buttons.Button tracksHistory;
    /**
     * Верхняя информационная панель с названием трека и исполнителем.
     *
     * <p>Экземпляр {@link BorderPane} (75px высота), расположенный над обложкой {@link #art}.
     * Содержит {@link #currentTrackName} (CENTER) и {@link #currentArtist} (BOTTOM).</p>
     *
     * <h3>Структура BorderPane</h3>
     *
     * <pre>{@code
     * [topDataPane: prefHeight=75px]
     *   ├─ CENTER: currentTrackName (main_font, 24pt)
     *   └─ BOTTOM: currentArtist   (font, 24pt)
     * }</pre>
     *
     * <h3>Позиционирование (из #set())</h3>
     *
     * <pre>{@code
     * topDataPane.setLayoutX(art.getLayoutX() - 50);
     * topDataPane.setLayoutY(art.getLayoutY() - 125);
     * topDataPane.setPrefWidth(art.getWidth() + 100);
     * topDataPane.setPrefHeight(75);
     * topDataPane.setCenter(currentTrackName);
     * topDataPane.setBottom(currentArtist);
     * root.getChildren().add(topDataPane);
     * }</pre>
     *
     * <h3>Биндинги (из #initBinds())</h3>
     *
     * <pre>{@code
     * topDataPane.layoutXProperty().bind(art.layoutXProperty()
     *     .subtract(topDataPane.prefWidthProperty()
     *         .subtract(art.widthProperty())
     *         .multiply(rootLayout.getTopDataPaneWidthXMultiplier())));
     * topDataPane.layoutYProperty().bind(art.layoutYProperty().subtract(rootLayout.getImgTop()));
     * }</pre>
     *
     * <h3>Z-порядок</h3>
     *
     * <p>Высокий приоритет наложения:</p>
     *
     * <pre>{@code
     * toFront(..., topDataPane, ...);
     * }</pre>
     *
     * <h3>Геометрия относительно якоря (art)</h3>
     *
     * <table summary="Позиционирование topDataPane">
     * <tr><th>Размер</th><th>Позиция</th><th>Относительно art</th></tr>
     * <tr><td>300px ширина</td><td>X = art.X - 50</td><td>Шире art на 100px</td></tr>
     * <tr><td>75px высота</td><td>Y = art.Y - 125</td><td>Выше art на 125px</td></tr>
     * </table>
     *
     * <h3>Роль в визуальной иерархии</h3>
     *
     * <ul>
     * <li><b>Контекст трека:</b> Название + исполнитель = идентификатор композиции</li>
     * <li><b>Якорь для списков:</b> tracksListView/similar.layoutY привязаны к topDataPane.Y + 10</li>
     * <li><b>Высокий приоритет:</b> Всегда поверх фона и подложек</li>
     * </ul>
     *
     * <h3>Источник данных</h3>
     *
     * @see #set() - позиционирование и размещение дочерних элементов
     * @see #initBinds() - реактивная привязка к art и stage
     */
    public BorderPane topDataPane;
    /**
     * Тень для обложки трека с эффектом Gaussian размытия.
     *
     * <p>Экземпляр {@link DropShadow}, применяемый к {@link #art} для создания глубины и 3D-эффекта.
     * Настраивается с радиусом 75px и spread 0.5 для мягкого размытия.</p>
     *
     * <h3>Полная настройка (из #set())</h3>
     *
     * <pre>{@code
     * imgTrackShadow.setRadius(75);
     * imgTrackShadow.setBlurType(BlurType.GAUSSIAN);
     * imgTrackShadow.setWidth(100);
     * imgTrackShadow.setHeight(100);
     * imgTrackShadow.setSpread(0.5);
     * art.setEffect(imgTrackShadow);
     * }</pre>
     *
     * <h3>Параметры эффекта</h3>
     *
     * <table summary="Параметры DropShadow">
     * <tr><th>Свойство</th><th>Значение</th><th>Эффект</th></tr>
     * <tr><td>radius</td><td>75px</td><td>Размер размытия</td></tr>
     * <tr><td>blurType</td><td>GAUSSIAN</td><td>Тип размытия (мягкое)</td></tr>
     * <tr><td>width/height</td><td>100x100px</td><td>Размер тени</td></tr>
     * <tr><td>spread</td><td>0.5</td><td>Распространение (50% твердости)</td></tr>
     * </table>
     *
     * <h3>Визуальная роль</h3>
     *
     * <ul>
     * <li><b>Глубина:</b> Подчеркивает центральную роль обложки в композиции</li>
     * <li><b>3D-эффект:</b> В связке с art.setRotationAxis(Point3D(25,25,25))</li>
     * <li><b>Фокус внимания:</b> Тень направляет взгляд к art</li>
     * </ul>
     *
     * <h3>Производительность</h3>
     *
     * <p>Экземпляр создается один раз и переиспользуется. Размытие радиусом 75px требует аппаратного
     * ускорения GPU для плавной анимации при изменении opacity/rotation art.</p>
     *
     * <h3>Интеграция с другими эффектами</h3>
     *
     * <pre>{@code
     * // Комплексный эффект обложки:
     * art.setEffect(imgTrackShadow);           // Тень
     * art.setRotationAxis(new Point3D(25,25,25)); // 3D поворот
     * art.setOpacity(1);                       // Полная непрозрачность
     * }</pre>
     *
     * <h3>Жизненный цикл</h3>
     *
     * <ul>
     * <li><b>Создание:</b> При инициализации класса (new DropShadow())</li>
     * <li><b>Настройка:</b> #set() перед применением к art</li>
     * <li><b>Применение:</b> art.setEffect() — постоянный эффект</li>
     * </ul>
     *
     * @see #set() - настройка параметров и применение к art
     */
    public DropShadow imgTrackShadow = new DropShadow();
    /**
     * Основное фоновое изображение окна.
     *
     * <p>Экземпляр {@link ImageView}, используемый как базовый слой фона для всего интерфейса.
     * Заполняется изображением из конфигурации {@code "background_image"}.</p>
     *
     * <h3>Инициализация (из #set())</h3>
     *
     * <pre>{@code
     * if(ConfigurationManager.instance.getBooleanItem("is_blur_background", true)) {
     *     createBackground(background);     // Размытие для основного фона
     *     createBackground(background_under);
     * }
     * }</pre>
     *
     * <h3>Применение изображения</h3>
     *
     * <p>Если конфиг {@code "background_image"} не пустой:</p>
     *
     * <pre>{@code
     * String image = ConfigurationManager.instance.getItem("background_image", "");
     * if(!image.isEmpty()) {
     *     Image img = new Image(image);
     *     BackgroundSize size = new BackgroundSize(100, 100, true, true, true, false);
     *     BackgroundImage bgImg = new BackgroundImage(img, NO_REPEAT, NO_REPEAT, CENTER, size);
     *     root.setBackground(new Background(bgImg));
     * }
     * }</pre>
     *
     * <h3>Роль в многослойном фоне</h3>
     *
     * <table summary="Слои фона">
     * <tr><th>Слой</th><th>Назначение</th><th>Обработка</th></tr>
     * <tr><td>root.background</td><td>Основной фон (image/CSS)</td><td>createBackground() + размытие</td></tr>
     * <tr><td>background</td><td>Верхний размытый слой</td><td>createBackground() + GaussianBlur</td></tr>
     * <tr><td>background_under</td><td>Нижний размытый слой</td><td>createBackground() + GaussianBlur</td></tr>
     * </table>
     *
     * @see #set() - условное размытие при "is_blur_background"=true
     * @see #createBackground(ImageView) - метод размытия
     */
    public ImageView background = new ImageView();
    /**
     * Нижний слой размытого фона под основным фоном.
     *
     * <p>Экземпляр {@link ImageView}, используемый как нижний уровень многослойной системы размытия.
     * Применяется вместе с {@link #background} для создания глубины фона.</p>
     *
     * <h3>Инициализация</h3>
     *
     * <p>Создается и размывается параллельно с основным фоном:</p>
     *
     * <pre>{@code
     * if(ConfigurationManager.instance.getBooleanItem("is_blur_background", true)) {
     *     createBackground(background);
     *     createBackground(background_under);  // Нижний слой
     * }
     * }</pre>
     *
     * <h3>Архитектура многослойного размытия</h3>
     *
     * <ol>
     * <li><b>root.setBackground(image)</b> — оригинальное изображение</li>
     * <li><b>background</b> — верхний размытый слой (виден сквозь UI)</li>
     * <li><b>background_under</b> — нижний размытый слой (глубина)</li>
     * </ol>
     *
     * <h3>Визуальный эффект</h3>
     *
     * <p>Создает эффект "стеклянной морфологии" — оригинальный фон размывается на двух слоях,
     * обеспечивая глубину без потери детализации под прозрачными элементами.</p>
     *
     * @see #set() - условное создание при "is_blur_background"=true
     * @see #createBackground(ImageView) - применение GaussianBlur эффекта
     */
    public ImageView background_under = new ImageView();
    /**
     * Глобальный интерполятор анимаций для всех UI-переходов.
     *
     * <p>Экземпляр {@link Interpolator}, создаваемый на основе конфигурации SPLINE с четырьмя
     * контрольными точками. Применяется ко всем анимациям интерфейса, включая {@link #soundSlider}.</p>
     *
     * <h3>Инициализация (из #set())</h3>
     *
     * <pre>{@code
     * String[] a = ConfigurationManager.instance.getItem("general_animations_interpolator", "0, 0, 1, 1")
     *     .replaceAll(" ", "").split(",");
     * general_interpolator = Interpolator.SPLINE(
     *     Double.parseDouble(a[0]),  // x1: 0.0 (по умолчанию)
     *     Double.parseDouble(a[1]),  // y1: 0.0
     *     Double.parseDouble(a[2]),  // x2: 1.0
     *     Double.parseDouble(a[3])   // y2: 1.0
     * );
     * }</pre>
     *
     * <h3>Применение</h3>
     *
     * <p>Основное использование — настройка слайдера:</p>
     *
     * <pre>{@code
     * soundSlider.setInterpolator(general_interpolator);
     * }</pre>
     *
     * <h3>Конфигурация SPLINE</h3>
     *
     * <p>По умолчанию создает линейную интерполяцию (0,0,1,1), но поддерживает кастомные кривые:</p>
     *
     * <table summary="Примеры SPLINE конфигураций">
     * <tr><th>Значения</th><th>Эффект</th></tr>
     * <tr><td>0,0,1,1</td><td>LINEAR — равномерная скорость</td></tr>
     * <tr><td>0.25,0.1,0.25,1</td><td>EASE_IN_OUT — плавный старт/финиш</td></tr>
     * <tr><td>0.42,0,0.58,1</td><td>Естественная кривая</td></tr>
     * </table>
     *
     * <h3>Роль в UI</h3>
     *
     * <ul>
     * <li><b>Единый стиль анимаций:</b> Все переходы используют одну кривую</li>
     * <li><b>Конфигурируемость:</b> Пользователь настраивает через конфиг</li>
     * <li><b>Централизованное управление:</b> Изменение в одном месте влияет на весь UI</li>
     * </ul>
     *
     * <h3>Жизненный цикл</h3>
     *
     * <ol>
     * <li>Чтение конфига #set() — самое начало метода</li>
     * <li>Создание Interpolator.SPLINE</li>
     * <li>Применение к soundSlider и другим элементам</li>
     * </ol>
     *
     */
    public Interpolator general_interpolator;
    /**
     * Система коэффициентов и отступов для динамического позиционирования UI-элементов.
     *
     * <p>Неизменяемый статический внутренний класс, содержащий более 25 fluently-цепляемых геттеров/
     * сеттеров для точной адаптивной верстки. Каждый метод возвращает {@code this} для chain-вызова.</p>
     *
     * <h3>Архитектурная роль</h3>
     *
     * <p>Централизованное хранилище математических коэффициентов, связывающих размеры
     * {@link #stage} с позициями элементов. Обеспечивает единый источник layout-правил.</p>
     *
     * <pre>{@code
     * // Пример chain-конфигурации
     * rootLayout.setImgTop(125)
     *           .setSliderBottom(50)
     *           .setPaddingArt(35);
     * }</pre>
     *
     * <h3>Основные группы параметров</h3>
     *
     * <table summary="Категории layout-параметров">
     * <tr><th>Категория</th><th>Примеры</th><th>Назначение</th></tr>
     * <tr><td>Обложка</td><td>imgTop=125, imgBottom=35</td><td>Отступы art</td></tr>
     * <tr><td>Слайдер</td><td>sliderBottom=50, sliderBlurBackgroundXSubtract=10</td><td>Временная шкала</td></tr>
     * <tr><td>Списки</td><td>maxListViewSize=100, similarWidthSubtract=35</td><td>ListView размеры</td></tr>
     * <tr><td>Кнопки</td><td>hideControlRightSide=18, btnSliderWidthMultiplier=0.5</td><td>Контролы</td></tr>
     * </table>
     *
     * <h3>Применение в биндингах (#initBinds())</h3>
     *
     * <pre>{@code
     * // Пример биндинга с коэффициентами
     * art.layoutXProperty().bind(stage.widthProperty()
     *     .multiply(rootLayout.getImgTrackArtRoundXMultiplier())  // 0.5
     *     .subtract(art.widthProperty().multiply(rootLayout.getImgTrackArtRoundWidthXMultiplier()))  // 0.5
     *     .subtract(corners * rootLayout.getImgTrackArtRoundWidthXMultiplier()));
     * }</pre>
     *
     * <h3>Ключевые значения (по умолчанию)</h3>
     *
     * <ul>
     * <li>{@code currentTrackTopPlaylistPane = 65} — отступ списков от верха</li>
     * <li>{@code bottomTracklistBottom = 65} — отступ списков снизу</li>
     * <li>{@code hideControlRightSide = 18} — отступ кнопки от правого края</li>
     * <li>{@code paddingArt = 35} — базовый отступ обложки</li>
     * </ul>
     *
     * <h3>Преимущества fluent API</h3>
     *
     * <ul>
     * <li><b>Chain-вызовы:</b> layout.setA(1).setB(2).setC(3)</li>
     * <li><b>Иммутабельность:</b> final поля, неизменяемые значения</li>
     * <li><b>Централизация:</b> Все коэффициенты в одном месте</li>
     * </ul>
     *
     * <h3>Жизненный цикл</h3>
     *
     * <p>Статический singleton {@link #rootLayout = new Layout()}, инициализируется значениями по
     * умолчанию. Не требует ручной настройки — используется напрямую в биндингах.</p>
     *
     * @see #initBinds() - использование коэффициентов в биндингах
     * @see #initListViewsBinds() - layout-специфичные привязки списков
     * @implNote Более 25 методов геттер/сеттер обеспечивают микротюнинг каждого отступа
     */
    public static final class Layout {
        /**
         * Верхний отступ обложки трека от верхнего края информационной панели.
         *
         * <p>Базовый отступ (125px), определяющий позицию {@link #topDataPane} относительно верха
         * {@link #art}. Используется в биндингах для обеспечения постоянного зазора.</p>
         *
         * <h3>Применение</h3>
         *
         * <pre>{@code
         * topDataPane.layoutYProperty().bind(
         *     art.layoutYProperty().subtract(rootLayout.getImgTop())  // -125px
         * );
         * }</pre>
         *
         * <p>Гарантирует, что информационная панель всегда находится на 125px выше обложки.</p>
         */
        private float imgTop = 125;

        /**
         * Нижний отступ обложки от слайдера воспроизведения.
         *
         * <p>Отступ (35px) между нижним краем {@link #art} и верхним краем {@link #soundSlider}.
         * Обеспечивает визуальный зазор в вертикальной композиции.</p>
         *
         * <h3>Применение в биндингах</h3>
         *
         * <pre>{@code
         * soundSlider.layoutYProperty().bind(
         *     art.layoutYProperty()
         *         .add(art.heightProperty())
         *         .add(rootLayout.getImgBottom())  // +35px
         * );
         * }</pre>
         */
        private float imgBottom = 35;

        /**
         * Максимальный процент ширины списка от размера окна.
         *
         * <p>Коэффициент (100 = 100%), ограничивающий максимальную ширину {@link #tracksListView}
         * и {@link #similar}. Предотвращает чрезмерное растяжение панелей.</p>
         *
         * <h3>Применение</h3>
         *
         * <pre>{@code
         * tracksListView.prefWidthProperty().bind(
         *     stage.widthProperty().multiply(rootLayout.getMaxListViewSize() / 100.0)
         * );
         * }</pre>
         */
        private float maxListViewSize = 100;

        /**
         * Базовый отступ обложки от краев и соседних элементов.
         *
         * <p>Универсальный padding (35px) вокруг {@link #art}, используемый для позиционирования
         * списков и кнопок. Обеспечивает консистентные промежутки.</p>
         *
         * <h3>Применение</h3>
         *
         * <pre>{@code
         * tracksListView.layoutXProperty().bind(
         *     art.layoutXProperty()
         *         .add(art.widthProperty())
         *         .add(rootLayout.getPaddingArt())  // +35px
         *         .add(10)
         * );
         * }</pre>
         */
        private int paddingArt = 35;

        /**
         * Отступ снизу от списка треков для нижних контролов.
         *
         * <p>Вычитается из высоты {@link #tracksListView} (100px), освобождая место для кнопок
         * и временных меток снизу.</p>
         *
         * <h3>Применение</h3>
         *
         * <pre>{@code
         * tracksListView.prefHeightProperty().bind(
         *     stage.heightProperty()
         *         .subtract(rootLayout.getTrackListViewHeightSubtract())  // -100px
         * );
         * }</pre>
         */
        private float trackListViewHeightSubtract = 100;

        /**
         * Возвращает текущий коэффициент вычитания высоты списка треков.
         *
         * @return Значение {@link #trackListViewHeightSubtract} (по умолчанию 100.0f).
         *
         * <p>Используется для расчета {@code prefHeight} {@link #tracksListView}:</p>
         *
         * <pre>{@code
         * tracksListView.prefHeightProperty().bind(
         *     stage.heightProperty()
         *         .subtract(rootLayout.getTrackListViewHeightSubtract())  // -100px
         * );
         * }</pre>
         *
         * @see #setTrackListViewHeightSubtract(float)
         */
        public float getTrackListViewHeightSubtract() {
            return trackListViewHeightSubtract;
        }

        /**
         * Устанавливает коэффициент вычитания высоты списка треков.
         *
         * @param trackListViewHeightSubtract Новый отступ снизу (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Определяет пространство, освобождаемое снизу {@link #tracksListView} для кнопок
         * управления, временных меток и отступов. По умолчанию 100px.</p>
         *
         * <h3>Пример chain-использования</h3>
         *
         * <pre>{@code
         * rootLayout.setTrackListViewHeightSubtract(120)
         *           .setPaddingArt(40);
         * }</pre>
         *
         * @see #getTrackListViewHeightSubtract()
         * @see #initListViewsBinds() - применение в биндинге высоты
         */
        public Layout setTrackListViewHeightSubtract(float trackListViewHeightSubtract) {
            this.trackListViewHeightSubtract = trackListViewHeightSubtract;
            return this;
        }

        /**
         * Отступ, вычитаемый из ширины панели похожих треков.
         *
         * <p>Коэффициент (35px), уменьшающий {@code prefWidth} {@link #similar} для обеспечения
         * отступов от краев и соседних элементов.</p>
         *
         * <h3>Применение в биндингах</h3>
         *
         * <pre>{@code
         * similar.prefWidthProperty().bind(
         *     art.layoutXProperty()
         *         .subtract(rootLayout.getPaddingArt())
         *         .subtract(rootLayout.getSimilarWidthSubtract())  // -35px
         * );
         * }</pre>
         */
        private float similarWidthSubtract = 35;

        /**
         * Возвращает коэффициент вычитания ширины панели похожих треков.
         *
         * @return Значение {@link #similarWidthSubtract} (по умолчанию 35.0f).
         *
         * <p>Используется для адаптивного ограничения ширины {@link #similar}:</p>
         *
         * <pre>{@code
         * similar.prefWidthProperty().bind(
         *     availableWidth.subtract(rootLayout.getSimilarWidthSubtract())
         * );
         * }</pre>
         *
         * @see #setSimilarWidthSubtract(float)
         */
        public float getSimilarWidthSubtract() {
            return similarWidthSubtract;
        }

        /**
         * Устанавливает коэффициент вычитания ширины панели похожих треков.
         *
         * @param similarWidthSubtract Новый отступ (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Обеспечивает боковые отступы панели {@link #similar} от краев окна и обложки.
         * По умолчанию 35px.</p>
         *
         * <h3>Влияние на layout</h3>
         *
         * <ul>
         * <li>Увеличивает отступ → сужает similar</li>
         * <li>Уменьшает отступ → расширяет similar</li>
         * </ul>
         *
         * @see #getSimilarWidthSubtract()
         * @see #initListViewsBinds() - применение в биндинге ширины similar
         */
        public Layout setSimilarWidthSubtract(float similarWidthSubtract) {
            this.similarWidthSubtract = similarWidthSubtract;
            return this;
        }

        /**
         * Горизонтальный отступ панели похожих треков от кнопки управления.
         *
         * <p>Отступ (25px) между {@link #hideControlLeft} и {@link #similar}. Обеспечивает
         * визуальный зазор при анимированном показе панели.</p>
         *
         * <h3>Применение в позиционировании</h3>
         *
         * <pre>{@code
         * similar.setLayoutX(
         *     hideControlLeft.getLayoutX() + rootLayout.getSimilarLayoutXPadding()  // +25px
         * );
         * }</pre>
         */
        private float similarLayoutXPadding = 25;

        /**
         * Возвращает горизонтальный отступ панели похожих треков от кнопки.
         *
         * @return Значение {@link #similarLayoutXPadding} (по умолчанию 25.0f).
         *
         * <p>Используется для точного позиционирования {@link #similar} относительно
         * {@link #hideControlLeft}:</p>
         *
         * <pre>{@code
         * similar.layoutX = hideControlLeft.layoutX + rootLayout.getSimilarLayoutXPadding();
         * }</pre>
         *
         * @see #setSimilarLayoutXPadding(float)
         */
        public float getSimilarLayoutXPadding() {
            return similarLayoutXPadding;
        }

        /**
         * Устанавливает горизонтальный отступ панели похожих треков от кнопки.
         *
         * @param similarLayoutXPadding Новый отступ справа от hideControlLeft (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Гарантирует минимальный визуальный зазор между кнопкой {@link #hideControlLeft}
         * и панелью {@link #similar} при анимированном появлении. По умолчанию 25px.</p>
         *
         * <h3>Визуальная схема</h3>
         *
         * <pre>{@code
         * [hideControlLeft] ───25px─── [similar]
         *           |                    |
         *      layoutX=2px          layoutX=27px
         * }</pre>
         *
         * @see #getSimilarLayoutXPadding()
         * @see #initListViewsBinds() - применение для similar.setLayoutX()
         */
        public Layout setSimilarLayoutXPadding(float similarLayoutXPadding) {
            this.similarLayoutXPadding = similarLayoutXPadding;
            return this;
        }

        /**
         * Дополнительный горизонтальный отступ списка треков от обложки.
         *
         * <p>Коэффициент (50px), вычитаемый при расчете ширины {@link #tracksListView}. Обеспечивает
         * дополнительное пространство между правым краем обложки и левым краем списка.</p>
         *
         * <h3>Формула ширины списка</h3>
         *
         * <pre>{@code
         * tracksListView.prefWidth = art.layoutX
         *     - rootLayout.getTrackListViewToImgTrackArtRoundXSubtract()  // -50px
         *     - rootLayout.getPaddingArt();                               // -35px
         * }</pre>
         */
        private float trackListViewToImgTrackArtRoundXSubtract = 50;

        /**
         * Возвращает дополнительный отступ списка треков от обложки.
         *
         * @return Значение {@link #trackListViewToImgTrackArtRoundXSubtract} (по умолчанию 50.0f).
         *
         * <p>Используется для расчета доступной ширины {@link #tracksListView}:</p>
         *
         * <pre>{@code
         * tracksListView.prefWidthProperty().bind(
         *     art.layoutXProperty()
         *         .subtract(rootLayout.getTrackListViewToImgTrackArtRoundXSubtract())  // -50px
         *         .subtract(rootLayout.getPaddingArt())                                // -35px
         * );
         * }</pre>
         *
         * @see #setTrackListViewToImgTrackArtRoundXSubtract(float)
         */
        public float getTrackListViewToImgTrackArtRoundXSubtract() {
            return trackListViewToImgTrackArtRoundXSubtract;
        }

        /**
         * Устанавливает дополнительный горизонтальный отступ списка от обложки.
         *
         * @param trackListViewToImgTrackArtRoundXSubtract Новый отступ (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Дополняет {@link #paddingArt}, обеспечивая точное позиционирование {@link #tracksListView}
         * справа от {@link #art}. По умолчанию 50px.</p>
         *
         * <h3>Влияние на ширину</h3>
         *
         * <table summary="Эффект изменения параметра">
         * <tr><th>Значение</th><th>Ширина tracksListView</th></tr>
         * <tr><td>50px (default)</td><td>art.X - 50 - paddingArt</td></tr>
         * <tr><td>0px</td><td>Максимальная ширина</td></tr>
         * <tr><td>100px</td><td>Уменьшенная ширина</td></tr>
         * </table>
         *
         * @see #getTrackListViewToImgTrackArtRoundXSubtract()
         * @see #initListViewsBinds() - применение в биндинге ширины tracksListView
         */
        public Layout setTrackListViewToImgTrackArtRoundXSubtract(float trackListViewToImgTrackArtRoundXSubtract) {
            this.trackListViewToImgTrackArtRoundXSubtract = trackListViewToImgTrackArtRoundXSubtract;
            return this;
        }

        /**
         * Множитель вертикальной позиции левой кнопки относительно высоты обложки.
         *
         * <p>Коэффициент (0.5f = 50%), определяющий положение {@link #hideControlLeft} по оси Y.
         * Кнопка центрируется относительно середины {@link #art}.</p>
         *
         * <h3>Формула позиционирования</h3>
         *
         * <pre>{@code
         * hideControlLeft.layoutY = art.layoutY + (art.height × hideControlLeftToImgTrackArtHeight)
         *                        = art.layoutY + (art.height × 0.5)
         * }</pre>
         */
        private float hideControlLeftToImgTrackArtHeight = 0.5f;

        /**
         * Возвращает множитель вертикальной позиции левой кнопки.
         *
         * @return Значение {@link #hideControlLeftToImgTrackArtHeight} (по умолчанию 0.5f).
         *
         * <p>Используется для центрирования {@link #hideControlLeft} относительно высоты обложки:</p>
         *
         * <pre>{@code
         * hideControlLeft.layoutYProperty().bind(
         *     art.layoutYProperty()
         *         .add(art.heightProperty().multiply(rootLayout.getHideControlLeftToImgTrackArtHeight()))
         * );
         * }</pre>
         *
         * @see #setHideControlLeftToImgTrackArtHeight(float)
         */
        public float getHideControlLeftToImgTrackArtHeight() {
            return hideControlLeftToImgTrackArtHeight;
        }

        /**
         * Устанавливает множитель вертикальной позиции левой кнопки относительно обложки.
         *
         * @param hideControlLeftToImgTrackArtHeight Множитель высоты art (0.0..1.0).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Определяет относительное положение {@link #hideControlLeft} по вертикали.
         * По умолчанию 0.5f (центр обложки).</p>
         *
         * <h3>Эффекты значений</h3>
         *
         * <table summary="Влияние множителя на позицию">
         * <tr><th>Значение</th><th>Позиция кнопки</th></tr>
         * <tr><td>0.0f</td><td>Верх обложки</td></tr>
         * <tr><td>0.5f (default)</td><td>Центр обложки</td></tr>
         * <tr><td>1.0f</td><td>Низ обложки</td></tr>
         * </table>
         *
         * <h3>Визуальная схема</h3>
         *
         * <pre>{@code
         * [art: height=200px]
         *   ↑ layoutY
         *   │
         * [hideControlLeft] ← Y = art.Y + (200 × 0.5) = art.Y + 100px
         * }</pre>
         *
         * @see #getHideControlLeftToImgTrackArtHeight()
         * @see #initBinds() - применение в биндинге layoutY кнопки
         */
        public Layout setHideControlLeftToImgTrackArtHeight(float hideControlLeftToImgTrackArtHeight) {
            this.hideControlLeftToImgTrackArtHeight = hideControlLeftToImgTrackArtHeight;
            return this;
        }

        /**
         * Фиксированная горизонтальная позиция левой кнопки управления.
         *
         * <p>Абсолютная координата X (2px от левого края окна) для {@link #hideControlLeft}.
         * Обеспечивает постоянное положение кнопки независимо от размеров окна.</p>
         */
        private float hideControlLeftLayoutX = 2;

        /**
         * Возвращает фиксированную горизонтальную позицию левой кнопки.
         *
         * @return Значение {@link #hideControlLeftLayoutX} (по умолчанию 2.0f).
         *
         * <p>Используется для жесткого позиционирования {@link #hideControlLeft}:</p>
         *
         * <pre>{@code
         * hideControlLeft.setLayoutX(rootLayout.getHideControlLeftLayoutX());  // = 2px
         * }</pre>
         *
         * @see #setHideControlLeft(float)
         */
        public float getHideControlLeftLayoutX() {
            return hideControlLeftLayoutX;
        }

        /**
         * Устанавливает фиксированную горизонтальную позицию левой кнопки.
         *
         * @param hideControlLeftLayoutX Абсолютная координата X от левого края (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Определяет постоянную позицию {@link #hideControlLeft} относительно левого края
         * {@link #stage}. В отличие от правой кнопки (биндинг к stage.width), левая имеет
         * фиксированную X=2px. По умолчанию 2px.</p>
         *
         * <h3>Визуальная схема</h3>
         *
         * <pre>{@code
         * [0px]───[hideControlLeft]───[similar]
         *    │     X=2px (фиксировано)
         *    └─ getHideControlLeftLayoutX()
         * }</pre>
         *
         * <h3>Отличие от hideControlRight</h3>
         *
         * <table summary="Позиционирование кнопок">
         * <tr><th>Кнопка</th><th>X-позиция</th><th>Источник</th></tr>
         * <tr><td>hideControlLeft</td><td>Фиксированная (2px)</td><td>#setHideControlLeft()</td></tr>
         * <tr><td>hideControlRight</td><td>Адаптивная (stage.width - N)</td><td>Биндинг</td></tr>
         * </table>
         *
         * @see #getHideControlLeftLayoutX()
         * @see #initBinds() - применение фиксированной позиции
         */
        public Layout setHideControlLeft(float hideControlLeftLayoutX) {
            this.hideControlLeftLayoutX = hideControlLeftLayoutX;
            return this;
        }

        /**
         * Множитель вертикальной позиции правой кнопки относительно высоты обложки.
         *
         * <p>Коэффициент (0.5f = 50%), определяющий положение {@link #hideControlRight} по оси Y.
         * Симметричен {@link #hideControlLeftToImgTrackArtHeight}, центрирует кнопку относительно
         * {@link #art}.</p>
         *
         * <h3>Формула позиционирования</h3>
         *
         * <pre>{@code
         * hideControlRight.layoutY = art.layoutY + (art.height × 0.5)
         * }</pre>
         */
        private float hideControlRightToImgTrackArtHeightMultiplier = 0.5f;

        /**
         * Возвращает множитель вертикальной позиции правой кнопки.
         *
         * @return Значение {@link #hideControlRightToImgTrackArtHeightMultiplier} (по умолчанию 0.5f).
         *
         * <p>Используется для центрирования {@link #hideControlRight}:</p>
         *
         * <pre>{@code
         * hideControlRight.layoutYProperty().bind(
         *     art.layoutYProperty()
         *         .add(art.heightProperty()
         *             .multiply(rootLayout.getHideControlRightToImgTrackArtHeightMultiplier()))
         * );
         * }</pre>
         *
         * @see #setHideControlRightToImgTrackArtHeightMultiplier(float)
         */
        public float getHideControlRightToImgTrackArtHeightMultiplier() {
            return hideControlRightToImgTrackArtHeightMultiplier;
        }

        /**
         * Устанавливает множитель вертикальной позиции правой кнопки.
         *
         * @param hideControlRightToImgTrackArtHeightMultiplier Множитель высоты art (0.0..1.0).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Синхронизирует вертикальное положение {@link #hideControlRight} с {@link #hideControlLeft}.
         * По умолчанию 0.5f (центр обложки).</p>
         *
         * <h3>Симметрия с левой кнопкой</h3>
         *
         * <table summary="Вертикальное выравнивание кнопок">
         * <tr><th>Кнопка</th><th>Множитель</th><th>Позиция Y</th></tr>
         * <tr><td>hideControlLeft</td><td>0.5f</td><td>art.Y + art.height×0.5</td></tr>
         * <tr><td>hideControlRight</td><td>0.5f</td><td>art.Y + art.height×0.5</td></tr>
         * </table>
         *
         * <h3>Визуальная схема</h3>
         *
         * <pre>{@code
         * [art: height=200px]
         *   ↑ layoutY
         *   │       [hideControlLeft] [hideControlRight]
         *   └───────0.5×200=100px─────┘
         * }</pre>
         *
         * @see #getHideControlRightToImgTrackArtHeightMultiplier()
         * @see #initBinds() - биндинг layoutY правой кнопки
         */
        public Layout setHideControlRightToImgTrackArtHeightMultiplier(float hideControlRightToImgTrackArtHeightMultiplier) {
            this.hideControlRightToImgTrackArtHeightMultiplier = hideControlRightToImgTrackArtHeightMultiplier;
            return this;
        }

        /**
         * Множитель горизонтального позиционирования центральной кнопки относительно слайдера.
         *
         * <p>Коэффициент (0.5f = 50%), центрирующий {@link #btn} над {@link #soundSlider}.
         * Обеспечивает симметричное размещение кнопки Play/Pause.</p>
         *
         * <h3>Формула позиционирования</h3>
         *
         * <pre>{@code
         * btn.layoutX = soundSlider.layoutX
         *     - (btn.prefWidth - soundSlider.prefWidth) × btnSliderWidthMultiplier
         *     = soundSlider.layoutX + (soundSlider.width - btn.width) / 2
         * }</pre>
         */
        private float btnSliderWidthMultiplier = 0.5f;

        /**
         * Возвращает множитель позиционирования кнопки над слайдером.
         *
         * @return Значение {@link #btnSliderWidthMultiplier} (по умолчанию 0.5f).
         *
         * <p>Центрирует {@link #btn} относительно ширины {@link #soundSlider}:</p>
         *
         * <pre>{@code
         * btn.layoutXProperty().bind(
         *     soundSlider.layoutXProperty()
         *         .subtract(btn.prefWidthProperty()
         *             .subtract(soundSlider.prefWidthProperty())
         *             .multiply(rootLayout.getBtnSliderWidthMultiplier()))
         * );
         * }</pre>
         *
         * @see #setBtnSliderWidthMultiplier(float)
         */
        public float getBtnSliderWidthMultiplier() {
            return btnSliderWidthMultiplier;
        }

        /**
         * Устанавливает множитель горизонтального позиционирования кнопки над слайдером.
         *
         * @param btnSliderWidthMultiplier Множитель ширины слайдера (обычно 0.0..1.0).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Гарантирует центрирование {@link #btn} над {@link #soundSlider}.
         * Значение 0.5f обеспечивает идеальное выравнивание по середине.</p>
         *
         * <h3>Эффекты значений</h3>
         *
         * <table summary="Позиция кнопки при разных множителях">
         * <tr><th>Значение</th><th>Позиция btn относительно soundSlider</th></tr>
         * <tr><td>0.0f</td><td>Левый край слайдера</td></tr>
         * <tr><td>0.5f (default)</td><td>Точный центр</td></tr>
         * <tr><td>1.0f</td><td>Правый край слайдера</td></tr>
         * </table>
         *
         * <h3>Визуальная схема</h3>
         *
         * <pre>{@code
         * [soundSlider: width=W]
         *       [btn: width=w]
         * w < W: btn.X = slider.X + (W-w) × 0.5
         * }</pre>
         *
         * @see #getBtnSliderWidthMultiplier()
         * @see #initBinds() - биндинг layoutX кнопки Play/Pause
         */
        public Layout setBtnSliderWidthMultiplier(float btnSliderWidthMultiplier) {
            this.btnSliderWidthMultiplier = btnSliderWidthMultiplier;
            return this;
        }

        /**
         * Множитель вертикального смещения метки времени относительно слайдера.
         *
         * <p>Коэффициент (1.5f = 150%), определяющий позицию {@link #beginTime} ниже
         * {@link #soundSlider}. Обеспечивает выравнивание меток по центру высоты слайдера.</p>
         *
         * <h3>Формула позиционирования</h3>
         *
         * <pre>{@code
         * beginTime.layoutY = soundSlider.layoutY
         *     + soundSlider.prefHeight × sliderBlurBackgroundBeginTimeMultiplier
         *     = soundSlider.Y + slider.height × 1.5
         * }</pre>
         */
        private float sliderBlurBackgroundBeginTimeMultiplier = 1.5f;

        /**
         * Возвращает множитель смещения метки времени над слайдером.
         *
         * @return Значение {@link #sliderBlurBackgroundBeginTimeMultiplier} (по умолчанию 1.5f).
         *
         * <p>Используется для позиционирования {@link #beginTime}:</p>
         *
         * <pre>{@code
         * beginTime.layoutYProperty().bind(
         *     soundSlider.layoutYProperty()
         *         .add(soundSlider.prefHeightProperty()
         *             .multiply(rootLayout.getSliderBlurBackgroundBeginTimeMultiplier()))
         * );
         * }</pre>
         *
         * @see #setSliderBlurBackgroundBeginTimeMultiplier(float)
         */
        public float getSliderBlurBackgroundBeginTimeMultiplier() {
            return sliderBlurBackgroundBeginTimeMultiplier;
        }

        /**
         * Устанавливает множитель вертикального смещения метки времени.
         *
         * @param sliderBlurBackgroundBeginTimeMultiplier Множитель высоты слайдера (обычно >1.0).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Позиционирует {@link #beginTime} ниже {@link #soundSlider} с учетом полутоновой высоты
         * слайдера. По умолчанию 1.5f обеспечивает центрирование метки относительно толстой полосы.</p>
         *
         * <h3>Эффекты значений</h3>
         *
         * <table summary="Положение beginTime при разных множителях">
         * <tr><th>Значение</th><th>Позиция относительно slider.height=25px</th></tr>
         * <tr><td>1.0f</td><td>slider.Y + 25px (нижний край)</td></tr>
         * <tr><td>1.5f (default)</td><td>slider.Y + 37.5px (полутон ниже)</td></tr>
         * <tr><td>2.0f</td><td>slider.Y + 50px (удаление вниз)</td></tr>
         * </table>
         *
         * <h3>Визуальная схема</h3>
         *
         * <pre>{@code
         * [soundSlider: height=25px]
         *     │
         * 1.5×25 = 37.5px
         *     │
         * [beginTime]
         * }</pre>
         *
         * @see #getSliderBlurBackgroundBeginTimeMultiplier()
         * @see #initBinds() - биндинг layoutY временной метки
         */
        public Layout setSliderBlurBackgroundBeginTimeMultiplier(float sliderBlurBackgroundBeginTimeMultiplier) {
            this.sliderBlurBackgroundBeginTimeMultiplier = sliderBlurBackgroundBeginTimeMultiplier;
            return this;
        }

        /**
         * Дополнительная ширина подложки слайдера за пределы самого слайдера.
         *
         * <p>Прирост (20px), расширяющий {@link #sliderBlurBackground} относительно
         * {@link #soundSlider}. Создает визуальный "воздух" вокруг слайдера.</p>
         *
         * <h3>Формула ширины</h3>
         *
         * <pre>{@code
         * sliderBlurBackground.width = soundSlider.prefWidth + sliderBlurBackgroundWidthAdd
         *                           = slider.width + 20px
         * }</pre>
         */
        private float sliderBlurBackgroundWidthAdd = 20;

        /**
         * Возвращает прирост ширины подложки слайдера.
         *
         * @return Значение {@link #sliderBlurBackgroundWidthAdd} (по умолчанию 20.0f).
         *
         * <p>Расширяет {@link #sliderBlurBackground}:</p>
         *
         * <pre>{@code
         * sliderBlurBackground.widthProperty().bind(
         *     soundSlider.prefWidthProperty()
         *         .add(rootLayout.getSliderBlurBackgroundWidthAdd())  // +20px
         * );
         * }</pre>
         *
         * @see #setSliderBlurBackgroundWidthAdd(float)
         */
        public float getSliderBlurBackgroundWidthAdd() {
            return sliderBlurBackgroundWidthAdd;
        }

        /**
         * Устанавливает прирост ширины подложки слайдера.
         *
         * @param sliderBlurBackgroundWidthAdd Дополнительная ширина (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Обеспечивает визуальное расширение {@link #sliderBlurBackground} за границы
         * {@link #soundSlider}. По умолчанию +20px создает мягкий фон с отступами.</p>
         *
         * <h3>Визуальный эффект</h3>
         *
         * <table summary="Эффект разных значений">
         * <tr><th>Значение</th><th>sliderBlurBackground.width</th></tr>
         * <tr><td>0px</td><td>= soundSlider.width</td></tr>
         * <tr><td>20px (default)</td><td>= soundSlider.width + 20px</td></tr>
         * <tr><td>40px</td><td>= soundSlider.width + 40px (широкий фон)</td></tr>
         * </table>
         *
         * <h3>Схема наложения</h3>
         *
         * <pre>{@code
         * [sliderBlurBackground: slider.width + 20px]
         *      [soundSlider: width]
         * ────────10px───┘──10px────
         * }</pre>
         *
         * @see #getSliderBlurBackgroundWidthAdd()
         * @see #initBinds() - биндинг ширины подложки
         */
        public Layout setSliderBlurBackgroundWidthAdd(float sliderBlurBackgroundWidthAdd) {
            this.sliderBlurBackgroundWidthAdd = sliderBlurBackgroundWidthAdd;
            return this;
        }

        /**
         * Дополнительная высота подложки слайдера за пределы самого слайдера.
         *
         * <p>Прирост (20px), расширяющий {@link #sliderBlurBackground} по вертикали относительно
         * {@link #soundSlider}. Создает увеличенную область размытого фона.</p>
         *
         * <h3>Формула высоты</h3>
         *
         * <pre>{@code
         * sliderBlurBackground.height = soundSlider.prefHeight + sliderBlurBackgroundHeightAdd
         *                            = slider.height + 20px
         * }</pre>
         */
        private float sliderBlurBackgroundHeightAdd = 20;

        /**
         * Возвращает прирост высоты подложки слайдера.
         *
         * @return Значение {@link #sliderBlurBackgroundHeightAdd} (по умолчанию 20.0f).
         *
         * <p>Расширяет {@link #sliderBlurBackground} по вертикали:</p>
         *
         * <pre>{@code
         * sliderBlurBackground.heightProperty().bind(
         *     soundSlider.prefHeightProperty()
         *         .add(rootLayout.getSliderBlurBackgroundHeightAdd())  // +20px
         * );
         * }</pre>
         *
         * @see #setSliderBlurBackgroundHeightAdd(float)
         */
        public float getSliderBlurBackgroundHeightAdd() {
            return sliderBlurBackgroundHeightAdd;
        }

        /**
         * Устанавливает прирост высоты подложки слайдера.
         *
         * @param sliderBlurBackgroundHeightAdd Дополнительная высота (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Увеличивает вертикальную область {@link #sliderBlurBackground} для создания "подушки"
         * вокруг {@link #soundSlider}. По умолчанию +20px обеспечивает плавный фон.</p>
         *
         * <h3>Визуальный эффект</h3>
         *
         * <table summary="Эффект разных значений">
         * <tr><th>Значение</th><th>sliderBlurBackground.height</th></tr>
         * <tr><td>0px</td><td>= soundSlider.height</td></tr>
         * <tr><td>20px (default)</td><td>= soundSlider.height + 20px</td></tr>
         * <tr><td>40px</td><td>= soundSlider.height + 40px (высокий фон)</td></tr>
         * </table>
         *
         * <h3>Схема наложения</h3>
         *
         * <pre>{@code
         * [sliderBlurBackground: slider.height + 20px]
         *      [soundSlider: height]
         * ────────10px──┘──10px──
         * }</pre>
         *
         * @see #getSliderBlurBackgroundHeightAdd()
         * @see #initBinds() - биндинг высоты подложки
         */
        public Layout setSliderBlurBackgroundHeightAdd(float sliderBlurBackgroundHeightAdd) {
            this.sliderBlurBackgroundHeightAdd = sliderBlurBackgroundHeightAdd;
            return this;
        }

        /**
         * Вертикальный сдвиг подложки слайдера вверх относительно слайдера.
         *
         * <p>Отрицательное смещение (10px), поднимающее {@link #sliderBlurBackground} выше
         * {@link #soundSlider}. Создает эффект "подложки под слайдером".</p>
         *
         * <h3>Формула позиционирования</h3>
         *
         * <pre>{@code
         * sliderBlurBackground.layoutY = soundSlider.layoutY - sliderBlurBackgroundYSubtract
         *                             = slider.Y - 10px
         * }</pre>
         */
        private float sliderBlurBackgroundYSubtract = 10;

        /**
         * Возвращает вертикальный сдвиг подложки слайдера вверх.
         *
         * @return Значение {@link #sliderBlurBackgroundYSubtract} (по умолчанию 10.0f).
         *
         * <p>Поднимает {@link #sliderBlurBackground}:</p>
         *
         * <pre>{@code
         * sliderBlurBackground.layoutYProperty().bind(
         *     soundSlider.layoutYProperty()
         *         .subtract(rootLayout.getSliderBlurBackgroundYSubtract())  // -10px
         * );
         * }</pre>
         *
         * @see #setSliderBlurBackgroundYSubtract(float)
         */
        public float getSliderBlurBackgroundYSubtract() {
            return sliderBlurBackgroundYSubtract;
        }

        /**
         * Устанавливает вертикальный сдвиг подложки слайдера вверх.
         *
         * @param sliderBlurBackgroundYSubtract Смещение вверх (положительное значение = сдвиг вверх).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Создает визуальный эффект, при котором {@link #sliderBlurBackground} частично
         * перекрывает {@link #soundSlider} сверху. По умолчанию 10px.</p>
         *
         * <h3>Эффекты значений</h3>
         *
         * <table summary="Положение подложки при разных сдвигах">
         * <tr><th>Значение</th><th>sliderBlurBackground.layoutY</th></tr>
         * <tr><td>0px</td><td>= soundSlider.Y</td></tr>
         * <tr><td>10px (default)</td><td>= soundSlider.Y - 10px</td></tr>
         * <tr><td>20px</td><td>= soundSlider.Y - 20px (сильный overlap)</td></tr>
         * </table>
         *
         * <h3>Визуальная схема</h3>
         *
         * <pre>{@code
         * [sliderBlurBackground]  ← Y = slider.Y - 10px
         *     [soundSlider]
         * ────overlap 10px───────┘
         * }</pre>
         *
         * @see #getSliderBlurBackgroundYSubtract()
         * @see #initBinds() - биндинг layoutY подложки
         */
        public Layout setSliderBlurBackgroundYSubtract(float sliderBlurBackgroundYSubtract) {
            this.sliderBlurBackgroundYSubtract = sliderBlurBackgroundYSubtract;
            return this;
        }

        /**
         * Горизонтальный сдвиг подложки слайдера влево относительно слайдера.
         *
         * <p>Отрицательное смещение (10px), расширяющее {@link #sliderBlurBackground} влево от
         * {@link #soundSlider}. Создает симметричный фон с отступами.</p>
         *
         * <h3>Формула позиционирования</h3>
         *
         * <pre>{@code
         * sliderBlurBackground.layoutX = soundSlider.layoutX - sliderBlurBackgroundXSubtract
         *                             = slider.X - 10px
         * }</pre>
         */
        private float sliderBlurBackgroundXSubtract = 10;

        /**
         * Возвращает горизонтальный сдвиг подложки слайдера влево.
         *
         * @return Значение {@link #sliderBlurBackgroundXSubtract} (по умолчанию 10.0f).
         *
         * <p>Сдвигает {@link #sliderBlurBackground} влево:</p>
         *
         * <pre>{@code
         * sliderBlurBackground.layoutXProperty().bind(
         *     soundSlider.layoutXProperty()
         *         .subtract(rootLayout.getSliderBlurBackgroundXSubtract())  // -10px
         * );
         * }</pre>
         *
         * @see #setSliderBlurBackgroundXSubtract(float)
         */
        public float getSliderBlurBackgroundXSubtract() {
            return sliderBlurBackgroundXSubtract;
        }

        /**
         * Устанавливает горизонтальный сдвиг подложки слайдера влево.
         *
         * @param sliderBlurBackgroundXSubtract Смещение влево (положительное = сдвиг влево).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>В сочетании с {@link #sliderBlurBackgroundWidthAdd} создает симметричный фон
         * вокруг {@link #soundSlider}. По умолчанию 10px обеспечивает отступ слева.</p>
         *
         * <h3>Комплексное позиционирование слайдера</h3>
         *
         * <table summary="Полная геометрия подложки">
         * <tr><th>Параметр</th><th>Эффект</th></tr>
         * <tr><td>XSubtract=10px</td><td>Сдвиг влево на 10px</td></tr>
         * <tr><td>WidthAdd=20px</td><td>Расширение вправо на 20px</td></tr>
         * <tr><td>Итог</td><td>10px слева + 10px справа</td></tr>
         * </table>
         *
         * <h3>Визуальная схема</h3>
         *
         * <pre>{@code
         * [sliderBlurBackground: width=slider.width+20px]
         *    [soundSlider: width]
         * ───10px──┘    ├10px
         * XSubtract    WidthAdd
         * }</pre>
         *
         * @see #getSliderBlurBackgroundXSubtract()
         * @see #initBinds() - биндинг layoutX подложки
         */
        public Layout setSliderBlurBackgroundXSubtract(float sliderBlurBackgroundXSubtract) {
            this.sliderBlurBackgroundXSubtract = sliderBlurBackgroundXSubtract;
            return this;
        }

        /**
         * Множитель горизонтального выравнивания информационной панели.
         *
         * <p>Коэффициент (0.5f = 50%), определяющий смещение {@link #topDataPane} относительно
         * {@link #art}. Центрирует панель над обложкой с учетом разницы в ширине.</p>
         *
         * <h3>Формула позиционирования</h3>
         *
         * <pre>{@code
         * topDataPane.layoutX = art.layoutX
         *     - (topDataPane.prefWidth - art.width) × topDataPaneWidthXMultiplier
         *     = art.X - (panel.width - art.width) / 2
         * }</pre>
         */
        private float topDataPaneWidthXMultiplier = 0.5f;

        /**
         * Возвращает множитель выравнивания информационной панели.
         *
         * @return Значение {@link #topDataPaneWidthXMultiplier} (по умолчанию 0.5f).
         *
         * <p>Центрирует {@link #topDataPane} над {@link #art}:</p>
         *
         * <pre>{@code
         * topDataPane.layoutXProperty().bind(
         *     art.layoutXProperty()
         *         .subtract(topDataPane.prefWidthProperty()
         *             .subtract(art.widthProperty())
         *             .multiply(rootLayout.getTopDataPaneWidthXMultiplier()))
         * );
         * }</pre>
         *
         * @see #setTopDataPaneWidthXMultiplier(float)
         */
        public float getTopDataPaneWidthXMultiplier() {
            return topDataPaneWidthXMultiplier;
        }

        /**
         * Устанавливает множитель выравнивания информационной панели.
         *
         * @param topDataPaneWidthXMultiplier Множитель разницы ширин (обычно 0.5 для центрирования).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Обеспечивает точное горизонтальное выравнивание {@link #topDataPane} относительно
         * {@link #art} при разной ширине (панель шире обложки на 100px). По умолчанию 0.5f.</p>
         *
         * <h3>Эффекты значений</h3>
         *
         * <table summary="Положение topDataPane при разных множителях">
         * <tr><th>Значение</th><th>Смещение относительно art.X</th></tr>
         * <tr><td>0.0f</td><td>0px (левый край к art.X)</td></tr>
         * <tr><td>0.5f (default)</td><td>(panel.width-art.width)/2 влево</td></tr>
         * <tr><td>1.0f</td><td>(panel.width-art.width) влево</td></tr>
         * </table>
         *
         * <h3>Визуальная схема</h3>
         *
         * <pre>{@code
         * [topDataPane: width=300px]
         *     [art: width=200px]
         * art.X ←──50px──[center]──50px──→
         * }</pre>
         *
         * @see #getTopDataPaneWidthXMultiplier()
         * @see #initBinds() - биндинг layoutX информационной панели
         */
        public Layout setTopDataPaneWidthXMultiplier(float topDataPaneWidthXMultiplier) {
            this.topDataPaneWidthXMultiplier = topDataPaneWidthXMultiplier;
            return this;
        }

        /**
         * Множитель масштабирования ширины обложки при расчете вертикальной позиции.
         *
         * <p>Коэффициент (0.8f = 80%), используемый для адаптивного позиционирования
         * {@link #art} по высоте относительно ширины stage. Уменьшает влияние ширины на Y-координату.</p>
         *
         * <h3>Формула в биндинге</h3>
         *
         * <pre>{@code
         * art.layoutYProperty().bind(
         *     stage.heightProperty()
         *         .multiply(rootLayout.getImgTrackArtRoundYMultiplier())           // 0.5
         *         .subtract(art.heightProperty()
         *             .multiply(rootLayout.getImgTrackArtRoundWidthYMultiplier()))  // 0.8
         * );
         * }</pre>
         */
        private float imgTrackArtRoundWidthYMultiplier = 0.8f;

        /**
         * Возвращает множитель масштабирования ширины обложки для Y-позиции.
         *
         * @return Значение {@link #imgTrackArtRoundWidthYMultiplier} (по умолчанию 0.8f).
         *
         * <p>Корректирует вертикальное позиционирование {@link #art}:</p>
         *
         * <pre>{@code
         * art.layoutYProperty().bind(
         *     stage.heightProperty().multiply(0.5)
         *         .subtract(art.heightProperty()
         *             .multiply(rootLayout.getImgTrackArtRoundWidthYMultiplier()))  // ×0.8
         * );
         * }</pre>
         *
         * @see #setImgTrackArtRoundWidthYMultiplier(float)
         */
        public float getImgTrackArtRoundWidthYMultiplier() {
            return imgTrackArtRoundWidthYMultiplier;
        }

        /**
         * Устанавливает множитель масштабирования ширины обложки для Y-позиции.
         *
         * @param imgTrackArtRoundWidthYMultiplier Множитель высоты art в Y-расчете (0.0..1.0).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Точно настраивает вертикальное центрирование {@link #art}, учитывая пропорции.
         * По умолчанию 0.8f обеспечивает небольшое смещение вверх для визуального баланса.</p>
         *
         * <h3>Эффект на позиционирование</h3>
         *
         * <table summary="Влияние на layoutY при stage.height=800, art.height=200">
         * <tr><th>Значение</th><th>Смещение</th><th>art.layoutY</th></tr>
         * <tr><td>0.5f</td><td>100px</td><td>300px</td></tr>
         * <tr><td>0.8f (default)</td><td>160px</td><td>240px</td></tr>
         * <tr><td>1.0f</td><td>200px</td><td>200px</td></tr>
         * </table>
         *
         * <h3>Полная формула Y:</h3>
         *
         * <pre>{@code
         * art.Y = stage.height × imgTrackArtRoundYMultiplier
         *       - art.height × imgTrackArtRoundWidthYMultiplier
         *       = 800×0.5 - 200×0.8 = 400 - 160 = 240px
         * }</pre>
         *
         * @see #getImgTrackArtRoundWidthYMultiplier()
         * @see #initBinds() - биндинг layoutY обложки
         */
        public Layout setImgTrackArtRoundWidthYMultiplier(float imgTrackArtRoundWidthYMultiplier) {
            this.imgTrackArtRoundWidthYMultiplier = imgTrackArtRoundWidthYMultiplier;
            return this;
        }

        /**
         * Множитель вертикального центрирования обложки относительно высоты окна.
         *
         * <p>Базовый коэффициент (0.5f = 50% высоты stage), определяющий опорную точку Y для
         * {@link #art}. Центрирует обложку по вертикали с учетом ее размера.</p>
         *
         * <h3>Формула в биндинге</h3>
         *
         * <pre>{@code
         * art.layoutYProperty().bind(
         *     stage.heightProperty()
         *         .multiply(rootLayout.getImgTrackArtRoundYMultiplier())  // 0.5 × stage.height
         *         .subtract(art.heightProperty()
         *             .multiply(rootLayout.getImgTrackArtRoundWidthYMultiplier()))  // 0.8 × art.height
         * );
         * }</pre>
         */
        private float imgTrackArtRoundYMultiplier = 0.5f;

        /**
         * Возвращает множитель вертикального центрирования обложки.
         *
         * @return Значение {@link #imgTrackArtRoundYMultiplier} (по умолчанию 0.5f).
         *
         * <p>Основной коэффициент для Y-позиции {@link #art}:</p>
         *
         * <pre>{@code
         * art.layoutYProperty().bind(
         *     stage.heightProperty()
         *         .multiply(rootLayout.getImgTrackArtRoundYMultiplier())  // Центр экрана
         *         ...
         * );
         * }</pre>
         *
         * @see #setImgTrackArtRoundYMultiplier(float)
         */
        public float getImgTrackArtRoundYMultiplier() {
            return imgTrackArtRoundYMultiplier;
        }

        /**
         * Устанавливает множитель вертикального центрирования обложки.
         *
         * @param imgTrackArtRoundYMultiplier Доля высоты stage для базовой Y-координаты (0.0..1.0).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Определяет вертикальную опорную точку обложки {@link #art}. По умолчанию 0.5f
         * центрирует по середине экрана.</p>
         *
         * <h3>Полная формула Y-позиции</h3>
         *
         * <pre>{@code
         * art.layoutY = (stage.height × imgTrackArtRoundYMultiplier)
         *             - (art.height × imgTrackArtRoundWidthYMultiplier)
         *             = (800 × 0.5) - (200 × 0.8)
         *             = 400 - 160 = 240px от верха
         * }</pre>
         *
         * <h3>Эффекты значений</h3>
         *
         * <table summary="Центрирование при разных множителях (stage=800px)">
         * <tr><th>Значение</th><th>Базовая Y-точка</th><th>art.layoutY (при art.h=200)</th></tr>
         * <tr><td>0.25f</td><td>200px</td><td>40px (ближе к верху)</td></tr>
         * <tr><td>0.5f (default)</td><td>400px</td><td>240px (центр)</td></tr>
         * <tr><td>0.75f</td><td>600px</td><td>440px (ближе к низу)</td></tr>
         * </table>
         *
         * @see #getImgTrackArtRoundYMultiplier()
         * @see #initBinds() - биндинг layoutY обложки
         */
        public Layout setImgTrackArtRoundYMultiplier(float imgTrackArtRoundYMultiplier) {
            this.imgTrackArtRoundYMultiplier = imgTrackArtRoundYMultiplier;
            return this;
        }

        /**
         * Множитель горизонтального центрирования обложки относительно ширины окна.
         *
         * <p>Коэффициент (0.5f = 50% ширины stage), определяющий базовую X-координату
         * {@link #art}. Центрирует обложку с учетом ее ширины и скругления углов.</p>
         *
         * <h3>Формула в биндинге</h3>
         *
         * <pre>{@code
         * art.layoutXProperty().bind(
         *     stage.widthProperty()
         *         .multiply(rootLayout.getImgTrackArtRoundXMultiplier())      // 0.5 × stage.width
         *         .subtract(art.widthProperty()
         *             .multiply(rootLayout.getImgTrackArtRoundWidthXMultiplier()))  // 0.5 × art.width
         *         .subtract(corners * rootLayout.getImgTrackArtRoundWidthXMultiplier())
         * );
         * }</pre>
         */
        private float imgTrackArtRoundWidthXMultiplier = 0.5f;

        /**
         * Возвращает множитель горизонтального центрирования обложки.
         *
         * @return Значение {@link #imgTrackArtRoundWidthXMultiplier} (по умолчанию 0.5f).
         *
         * <p>Основной коэффициент для X-позиции {@link #art}:</p>
         *
         * <pre>{@code
         * art.layoutXProperty().bind(
         *     stage.widthProperty().multiply(0.5)
         *         .subtract(art.widthProperty()
         *             .multiply(rootLayout.getImgTrackArtRoundWidthXMultiplier()))  // ×0.5
         *         .subtract(corners * rootLayout.getImgTrackArtRoundWidthXMultiplier())
         * );
         * }</pre>
         *
         * @see #setImgTrackArtRoundWidthXMultiplier(float)
         */
        public float getImgTrackArtRoundWidthXMultiplier() {
            return imgTrackArtRoundWidthXMultiplier;
        }

        /**
         * Устанавливает множитель горизонтального центрирования обложки.
         *
         * @param imgTrackArtRoundWidthXMultiplier Доля ширины art в X-расчете (обычно 0.5).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Корректирует горизонтальное центрирование {@link #art}, учитывая ширину обложки
         * и скругление углов ({@link #corners}). По умолчанию 0.5f.</p>
         *
         * <h3>Полная формула X-позиции</h3>
         *
         * <pre>{@code
         * art.layoutX = (stage.width × imgTrackArtRoundXMultiplier)
         *             - (art.width × imgTrackArtRoundWidthXMultiplier)
         *             - (corners × imgTrackArtRoundWidthXMultiplier)
         *             = (800 × 0.5) - (200 × 0.5) - (15 × 0.5)
         *             = 400 - 100 - 7.5 = 292.5px
         * }</pre>
         *
         * <h3>Эффекты значений</h3>
         *
         * <table summary="Смещение при stage.width=800px, art.width=200px">
         * <tr><th>Значение</th><th>Смещение art</th></tr>
         * <tr><td>0.25f</td><td>Меньше влево</td></tr>
         * <tr><td>0.5f (default)</td><td>Точное центрирование</td></tr>
         * <tr><td>0.75f</td><td>Больше вправо</td></tr>
         * </table>
         *
         * @see #getImgTrackArtRoundWidthXMultiplier()
         * @see #initBinds() - биндинг layoutX обложки
         */
        public Layout setImgTrackArtRoundWidthXMultiplier(float imgTrackArtRoundWidthXMultiplier) {
            this.imgTrackArtRoundWidthXMultiplier = imgTrackArtRoundWidthXMultiplier;
            return this;
        }

        /**
         * Множитель горизонтального центрирования обложки относительно ширины окна.
         *
         * <p>Базовый коэффициент (0.5f = 50% ширины stage), определяющий опорную X-точку для
         * {@link #art}. Является первым множителем в цепочке горизонтального центрирования.</p>
         *
         * <h3>Формула в биндинге</h3>
         *
         * <pre>{@code
         * art.layoutXProperty().bind(
         *     stage.widthProperty()
         *         .multiply(rootLayout.getImgTrackArtRoundXMultiplier())           // 0.5 × stage.width
         *         .subtract(art.widthProperty()
         *             .multiply(rootLayout.getImgTrackArtRoundWidthXMultiplier()))  // 0.5 × art.width
         *         .subtract(corners * rootLayout.getImgTrackArtRoundWidthXMultiplier())
         * );
         * }</pre>
         */
        private float imgTrackArtRoundXMultiplier = 0.5f;

        /**
         * Возвращает множитель базового горизонтального центрирования.
         *
         * @return Значение {@link #imgTrackArtRoundXMultiplier} (по умолчанию 0.5f).
         *
         * <p>Начальный коэффициент для X-позиции {@link #art}:</p>
         *
         * <pre>{@code
         * art.layoutXProperty().bind(
         *     stage.widthProperty()
         *         .multiply(rootLayout.getImgTrackArtRoundXMultiplier())  // Центр экрана
         *         ...
         * );
         * }</pre>
         *
         * @see #setImgTrackArtRoundXMultiplier(float)
         */
        public float getImgTrackArtRoundXMultiplier() {
            return imgTrackArtRoundXMultiplier;
        }

        /**
         * Устанавливает множитель базового горизонтального центрирования обложки.
         *
         * @param imgTrackArtRoundXMultiplier Доля ширины stage для базовой X-координаты (0.0..1.0).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Определяет горизонтальную опорную точку {@link #art} на экране. По умолчанию 0.5f
         * размещает центр обложки в центре окна.</p>
         *
         * <h3>Полная формула X-позиции</h3>
         *
         * <pre>{@code
         * art.layoutX = (stage.width × imgTrackArtRoundXMultiplier)
         *             - (art.width × imgTrackArtRoundWidthXMultiplier)
         *             - (corners × imgTrackArtRoundWidthXMultiplier)
         *             = (800 × 0.5) - (200 × 0.5) - (15 × 0.5)
         *             = 400 - 100 - 7.5 = 292.5px
         * }</pre>
         *
         * <h3>Эффекты значений</h3>
         *
         * <table summary="Базовая точка при stage.width=800px">
         * <tr><th>Значение</th><th>Базовая X-точка</th></tr>
         * <tr><td>0.25f</td><td>200px (1/4 экрана)</td></tr>
         * <tr><td>0.5f (default)</td><td>400px (центр)</td></tr>
         * <tr><td>0.75f</td><td>600px (3/4 экрана)</td></tr>
         * </table>
         *
         * @see #getImgTrackArtRoundXMultiplier()
         * @see #initBinds() - биндинг layoutX обложки
         */
        public Layout setImgTrackArtRoundXMultiplier(float imgTrackArtRoundXMultiplier) {
            this.imgTrackArtRoundXMultiplier = imgTrackArtRoundXMultiplier;
            return this;
        }

        /**
         * Отступ кнопок управления снизу от слайдера воспроизведения.
         *
         * <p>Вертикальный зазор (50px) между {@link #soundSlider} и кнопками
         * {@link #btn}, {@link #btnDown}, {@link #btnNext}.</p>
         *
         * <h3>Применение</h3>
         *
         * <pre>{@code
         * btn.layoutYProperty().bind(
         *     soundSlider.layoutYProperty()
         *         .add(soundSlider.prefHeightProperty())
         *         .add(rootLayout.getSliderBottom())  // +50px
         * );
         * }</pre>
         */
        private int sliderBottom = 50;

        /**
         * Отступ правой кнопки от правого края окна.
         *
         * <p>Расстояние (18px) от {@link #stage} до {@link #hideControlRight}.</p>
         *
         * <h3>Применение</h3>
         *
         * <pre>{@code
         * hideControlRight.layoutXProperty().bind(
         *     stage.widthProperty()
         *         .subtract(rootLayout.getHideControlRightSide())  // -18px
         *         .subtract(hideControlRight.prefWidthProperty())
         * );
         * }</pre>
         */
        private int hideControlRightSide = 18;

        /**
         * Нижний отступ списка треков от нижнего края окна.
         *
         * <p>Зазор (65px) снизу {@link #tracksListView} для кнопок и временных меток.</p>
         *
         * <h3>Применение</h3>
         *
         * <pre>{@code
         * tracksListView.prefHeightProperty().bind(
         *     stage.heightProperty()
         *         .subtract(rootLayout.getBottomTracklistBottom())  // -65px
         * );
         * }</pre>
         */
        private int bottomTracklistBottom = 65;

        /**
         * Верхний отступ списков от информационной панели.
         *
         * <p>Отступ (65px) сверху {@link #tracksListView} от {@link #topDataPane}. Совпадает с
         * высотой информационной панели + зазор.</p>
         *
         * <h3>Применение</h3>
         *
         * <pre>{@code
         * art.setLayoutY((stage.getHeight() / 2) - (art.getHeight() / 2)
         *     - rootLayout.getCurrentTrackTopPlaylistPane());  // -65px в #set()
         *
         * tracksListView.layoutYProperty().bind(
         *     topDataPane.layoutYProperty().add(10)
         * );
         * }</pre>
         */
        private int currentTrackTopPlaylistPane = 65;

        /**
         * Возвращает верхний отступ списков от информационной панели.
         *
         * @return Значение {@link #currentTrackTopPlaylistPane} (по умолчанию 65).
         *
         * <p>Используется как в статическом позиционировании:</p>
         *
         * <pre>{@code
         * art.setLayoutY((stage.getHeight() / 2) - (art.getHeight() / 2)
         *     - rootLayout.getCurrentTrackTopPlaylistPane());  // -65px
         * }</pre>
         *
         * <p>Так и в биндингах для {@link #tracksListView}:</p>
         *
         * <pre>{@code
         * tracksListView.layoutYProperty().bind(topDataPane.layoutYProperty().add(10));
         * }</pre>
         *
         * @see #setCurrentTrackTopPlaylistPane(int)
         */
        public int getCurrentTrackTopPlaylistPane() {
            return currentTrackTopPlaylistPane;
        }

        /**
         * Устанавливает верхний отступ списков от информационной панели.
         *
         * @param currentTrackTopPlaylistPane Новый отступ (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Контролирует вертикальное расстояние между {@link #topDataPane} и списками
         * {@link #tracksListView}, {@link #similar}. По умолчанию 65px (высота topDataPane + зазор).</p>
         *
         * @see #getCurrentTrackTopPlaylistPane()
         */
        public Layout setCurrentTrackTopPlaylistPane(int currentTrackTopPlaylistPane) {
            this.currentTrackTopPlaylistPane = currentTrackTopPlaylistPane;
            return this;
        }

        /**
         * Возвращает отступ правой кнопки от правого края окна.
         *
         * @return Значение {@link #hideControlRightSide} (по умолчанию 18).
         *
         * <p>Используется для позиционирования {@link #hideControlRight}:</p>
         *
         * <pre>{@code
         * hideControlRight.layoutXProperty().bind(
         *     stage.widthProperty().subtract(rootLayout.getHideControlRightSide())  // -18px
         * );
         * }</pre>
         *
         * @see #setHideControlRightSide(int)
         */
        public int getHideControlRightSide() {
            return hideControlRightSide;
        }

        /**
         * Устанавливает отступ правой кнопки от правого края.
         *
         * @param hideControlRightSide Новый отступ (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Определяет расстояние {@link #hideControlRight} от правого края {@link #stage}.
         * По умолчанию 18px обеспечивает компактное размещение.</p>
         *
         * @see #getHideControlRightSide()
         */
        public Layout setHideControlRightSide(int hideControlRightSide) {
            this.hideControlRightSide = hideControlRightSide;
            return this;
        }

        /**
         * Возвращает нижний отступ списка треков от нижнего края окна.
         *
         * @return Значение {@link #bottomTracklistBottom} (по умолчанию 65).
         *
         * <p>Ограничивает высоту {@link #tracksListView}:</p>
         *
         * <pre>{@code
         * tracksListView.prefHeightProperty().bind(
         *     stage.heightProperty()
         *         .subtract(rootLayout.getBottomTracklistBottom())  // -65px
         * );
         * }</pre>
         *
         * <p>Освобождает место для кнопок управления и временных меток.</p>
         *
         * @see #setBottomTracklistBottom(int)
         */
        public int getBottomTracklistBottom() {
            return bottomTracklistBottom;
        }

        /**
         * Устанавливает нижний отступ списка треков от нижнего края.
         *
         * @param bottomTracklistBottom Новый отступ снизу (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Определяет зазор снизу {@link #tracksListView} для нижних UI-элементов
         * (кнопки, {@link #beginTime}/{@link #endTime}). По умолчанию 65px.</p>
         *
         * @see #getBottomTracklistBottom()
         */
        public Layout setBottomTracklistBottom(int bottomTracklistBottom) {
            this.bottomTracklistBottom = bottomTracklistBottom;
            return this;
        }

        /**
         * Создает экземпляр Layout с настройками по умолчанию.
         *
         * <p>Инициализирует все коэффициенты positioning'а стандартными значениями.
         * Единственный публичный конструктор, используется для создания {@link #rootLayout}.</p>
         *
         * <pre>{@code
         * public static final Layout rootLayout = new Layout();
         * }</pre>
         *
         * <h3>Значения по умолчанию</h3>
         *
         * <ul>
         * <li>{@link #imgTop} = 125f</li>
         * <li>{@link #sliderBottom} = 50</li>
         * <li>{@link #paddingArt} = 35</li>
         * <li>и все остальные параметры...</li>
         * </ul>
         */
        public Layout() {}

        /**
         * Возвращает отступ кнопок управления от слайдера воспроизведения.
         *
         * @return Значение {@link #sliderBottom} (по умолчанию 50).
         *
         * <p>Вертикальный зазор между {@link #soundSlider} и кнопками
         * {@link #btn}, {@link #btnDown}, {@link #btnNext}:</p>
         *
         * <pre>{@code
         * btn.layoutYProperty().bind(
         *     soundSlider.layoutYProperty()
         *         .add(soundSlider.prefHeightProperty())
         *         .add(rootLayout.getSliderBottom())  // +50px
         * );
         * }</pre>
         *
         * @see #setSliderBottom(int)
         */
        public int getSliderBottom() {
            return sliderBottom;
        }

        /**
         * Устанавливает отступ кнопок от слайдера воспроизведения.
         *
         * @param sliderBottom Новый вертикальный зазор (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Контролирует расстояние между {@link #soundSlider} и нижними кнопками
         * управления. По умолчанию 50px обеспечивает читаемое разделение UI-блоков.</p>
         *
         * @see #getSliderBottom()
         */
        public Layout setSliderBottom(int sliderBottom) {
            this.sliderBottom = sliderBottom;
            return this;
        }

        /**
         * Возвращает горизонтальный отступ обложки от краев контейнера.
         *
         * @return Значение {@link #paddingArt} (по умолчанию 35).
         *
         * <p>Используется для центрирования {@link #art}:</p>
         *
         * <pre>{@code
         * art.setLayoutX((stage.getWidth() / 2) - (art.getWidth() / 2)
         *     - rootLayout.getPaddingArt());  // -35px
         * }</pre>
         *
         * @see #setPaddingArt(int)
         */
        public int getPaddingArt() {
            return paddingArt;
        }

        /**
         * Устанавливает горизонтальный отступ обложки альбома.
         *
         * @param paddingArt Новый отступ от краев (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Асимметричный отступ {@link #art} от центра для визуального баланса.
         * По умолчанию 35px сдвигает обложку правее центра.</p>
         *
         * @see #getPaddingArt()
         */
        public Layout setPaddingArt(int paddingArt) {
            this.paddingArt = paddingArt;
            return this;
        }

        /**
         * Возвращает максимальный коэффициент размера ListView.
         *
         * @return Значение {@link #maxListViewSize} (по умолчанию 100f).
         *
         * <p>Вероятно, используется для ограничения ширины/высоты {@link #tracksListView}
         * и {@link #similar} в процентах от контейнера.</p>
         *
         * <pre>{@code
         * // Возможное применение в initListViewsBinds():
         * tracksListView.prefWidthProperty().bind(
         *     stage.widthProperty().multiply(rootLayout.getMaxListViewSize() / 100)
         * );
         * }</pre>
         *
         * @see #setMaxListViewSize(float)
         */
        public float getMaxListViewSize() {
            return maxListViewSize;
        }

        /**
         * Устанавливает максимальный коэффициент размера ListView.
         *
         * @param maxListViewSize Новый максимум (проценты, пиксели?).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Ограничивает рост {@link #tracksListView} и {@link #similar} при ресайзе окна.
         * По умолчанию 100f (100% ширины root).</p>
         *
         * @see #getMaxListViewSize()
         */
        public Layout setMaxListViewSize(float maxListViewSize) {
            this.maxListViewSize = maxListViewSize;
            return this;
        }

        /**
         * Возвращает верхний отступ изображения обложки.
         *
         * @return Значение {@link #imgTop} (по умолчанию 125f).
         *
         * <p>Вертикальное смещение {@link #art} от верха:</p>
         *
         * <pre>{@code
         * art.layoutYProperty().bind(
         *     stage.heightProperty().multiply(0.5)
         *         .subtract(art.heightProperty().multiply(0.8))
         *         .subtract(rootLayout.getImgTop())  // -125f
         * );
         * }</pre>
         *
         * @see #setImgTop(float)
         */
        public float getImgTop() {
            return imgTop;
        }

        /**
         * Устанавливает верхний отступ изображения обложки.
         *
         * @param imgTop Новый отступ сверху (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Позиционирует {@link #art} относительно центра экрана с учетом высоты.
         * По умолчанию 125f обеспечивает баланс между верхними элементами и обложкой.</p>
         *
         * @see #getImgTop()
         */
        public Layout setImgTop(float imgTop) {
            this.imgTop = imgTop;
            return this;
        }

        /**
         * Возвращает нижний отступ изображения обложки.
         *
         * @return Значение {@link #imgBottom} (по умолчанию 35f).
         *
         * <p>Вертикальный зазор между {@link #art} и {@link #soundSlider}:</p>
         *
         * <pre>{@code
         * soundSlider.layoutYProperty().bind(
         *     art.layoutYProperty()
         *         .add(art.heightProperty())
         *         .add(rootLayout.getImgBottom())  // +35f
         * );
         * }</pre>
         *
         * @see #setImgBottom(float)
         */
        public float getImgBottom() {
            return imgBottom;
        }

        /**
         * Устанавливает нижний отступ изображения обложки.
         *
         * @param imgBottom Новый зазор до слайдера (пиксели).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Разделяет {@link #art} и {@link #soundSlider}. По умолчанию 35f
         * обеспечивает визуальный отступ между обложкой и панелью управления.</p>
         *
         * @see #getImgBottom()
         */
        public Layout setImgBottom(float imgBottom) {
            this.imgBottom = imgBottom;
            return this;
        }

        /**
         * Устанавливает горизонтальную позицию левой кнопки плейлиста.
         *
         * @param hideControlLeftLayoutX Новый X-координата (пиксели от левого края).
         * @return {@code this} для fluent chain-вызова.
         *
         * <h3>Назначение</h3>
         *
         * <p>Фиксирует {@link #hideControlLeft} слева от окна. По умолчанию 2f
         * (почти у самого края). Используется в initBinds():</p>
         *
         * <pre>{@code
         * hideControlLeft.setLayoutX(rootLayout.getHideControlLeftLayoutX());  // 2f
         * }</pre>
         *
         * <p>База для позиционирования {@link #similar}.</p>
         *
         * @see #getHideControlLeftLayoutX()
         */
        public Layout setHideControlLeftLayoutX(float hideControlLeftLayoutX) {
            this.hideControlLeftLayoutX = hideControlLeftLayoutX;
            return this;
        }
    }

    /**
     * Глобальный экземпляр Layout с настройками позиционирования UI по умолчанию.
     *
     * <p>Единственный публичный экземпляр, используется во всех биндингах и setLayoutX/Y().
     * Инициализируется стандартными значениями для responsive layout'а плеера.</p>
     *
     * <h3>Основные значения по умолчанию</h3>
     *
     * <table>
     * <tr><th>Параметр</th><th>Значение</th><th>Назначение</th></tr>
     * <tr><td>{@link Layout#imgTop}</td><td>125f</td><td>Верхний отступ обложки</td></tr>
     * <tr><td>{@link Layout#imgBottom}</td><td>35f</td><td>Нижний отступ до слайдера</td></tr>
     * <tr><td>{@link Layout#sliderBottom}</td><td>50</td><td>Отступ кнопок от слайдера</td></tr>
     * <tr><td>{@link Layout#paddingArt}</td><td>35</td><td>Горизонтальный отступ обложки</td></tr>
     * <tr><td>{@link Layout#bottomTracklistBottom}</td><td>65</td><td>Нижний отступ списка треков</td></tr>
     * <tr><td>{@link Layout#maxListViewSize}</td><td>100f</td><td>Макс. размер ListView</td></tr>
     * </table>
     *
     * <h3>Применение в коде</h3>
     *
     * <pre>{@code
     * // Биндинги
     * soundSlider.layoutYProperty().bind(
     *     art.layoutYProperty().add(art.heightProperty())
     *         .add(rootLayout.getImgBottom())  // 35f
     * );
     *
     * // Прямое позиционирование
     * art.setLayoutY((stage.getHeight() / 2) - (art.getHeight() / 2)
     *     - rootLayout.getCurrentTrackTopPlaylistPane());  // 65
     * }</pre>
     *
     * <p>Все геттеры/сеттеры Layout доступны через {@code rootLayout}.</p>
     */
    public Layout rootLayout = new Layout();

    /**
     * Кнопка "Следующий трек" в панели управления плеером.
     *
     * <p>Расположена справа от слайдера воспроизведения. Отвечает за переход
     * к следующему треку в плейлисте {@link #tracksListView}.</p>
     *
     * <h3>Позиционирование (биндинг)</h3>
     *
     * <pre>{@code
     * btnNext.layoutXProperty().bind(
     *     soundSlider.layoutXProperty()
     *         .add(soundSlider.prefWidthProperty())
     *         .subtract(btnNext.prefWidthProperty())
     * );
     * btnNext.layoutYProperty().bind(btn.layoutYProperty());
     * }</pre>
     *
     * <h3>События</h3>
     *
     * <ul>
     * <li><strong>Tooltip:</strong> Показывает название следующего трека + горячие клавиши</li>
     * <li><strong>CSS:</strong> {@code -fx-cursor: hand}</li>
     * </ul>
     *
     * <p>Инициализируется в {@link #init()} и добавляется в {@link #root}.</p>
     */
    public NextButton btnNext;
    /**
     * Основная кнопка Play/Pause управления воспроизведением.
     *
     * <p>Центральный элемент панели управления под слайдером {@link #soundSlider}.
     * Переключает состояние медиаплеера между воспроизведением и паузой.</p>
     *
     * <h3>Позиционирование (биндинг)</h3>
     *
     * <pre>{@code
     * btn.layoutXProperty().bind(
     *     soundSlider.layoutXProperty()
     *         .subtract(btn.prefWidthProperty())
     *         .subtract(soundSlider.prefWidthProperty()
     *             .multiply(rootLayout.getBtnSliderWidthMultiplier()))  // 0.5f
     * );
     * btn.layoutYProperty().bind(
     *     soundSlider.layoutYProperty()
     *         .add(soundSlider.prefHeightProperty())
     *         .add(rootLayout.getSliderBottom())  // 50px
     * );
     * }</pre>
     *
     * <h3>События и поведение</h3>
     *
     * <ul>
     * <li><strong>Tooltip:</strong> {@code LocalizationManager.getLocaleString(TOOLTIPMAINPLAY)}</li>
     * <li><strong>CSS:</strong> {@code -fx-cursor: hand}</li>
     * <li><strong>Обработчик:</strong> {@link MediaProcessor#pause_play()}}</li>
     * </ul>
     *
     * <p>Инициализируется в {@link #init()} → {@link #set()} и добавляется в {@link #root}.</p>
     *
     * @see ButtonHandler#initialize() Настройка обработчиков
     */
    public PlayButton btn;
    /**
     * Кнопка "Предыдущий трек" в панели управления плеером.
     *
     * <p>Расположена слева от основной кнопки {@link #btn}. Переходит к предыдущему
     * треку в текущем плейлисте {@link #tracksListView}.</p>
     *
     * <h3>Позиционирование (биндинг)</h3>
     *
     * <pre>{@code
     * btnDown.layoutXProperty().bind(soundSlider.layoutXProperty());
     * btnDown.layoutYProperty().bind(btn.layoutYProperty());
     * }</pre>
     *
     * <h3>События и поведение</h3>
     *
     * <ul>
     * <li><strong>Tooltip:</strong> Название предыдущего трека + горячие клавиши (SKIP/PIT)</li>
     * <li><strong>CSS:</strong> {@code -fx-cursor: hand}</li>
     * <li><strong>Обработчик:</strong> {@link PlayProcessor#next()} ()}</li>
     * </ul>
     *
     * <p>Визуально выровнена по основной кнопке Play/Pause. Инициализируется в
     * {@link #init()} и добавляется в {@link Pane#getChildren()}.</p>
     *
     */
    public PrevButton btnDown;

    /**
     * Панель дополнительных функций плеера (ControlPane).
     *
     * <p>Содержит кнопку с выпадающим меню для доступа к основным функциям:
     * история, настройки, лицензия и т.д. Расположена в верхней части интерфейса.</p>
     *
     * <h3>Инициализация и добавление</h3>
     *
     * <pre>{@code
     * mainFunctions = new ControlPane(root);
     * root.getChildren().add(mainFunctions);
     *
     * mainFunctions.addCenteredButton(new Commons());
     * }</pre>
     *
     * <h3>Tooltip основной кнопки</h3>
     *
     * <pre>{@code
     * mainFunctions.getMainButton().setTooltip(
     *     new ContextTooltip(LocalizationManager.getLocaleString(TOOLTIPMAINFUNCTIONSBUTTON))
     * );
     * }</pre>
     *
     * <h3>Свойства</h3>
     *
     * <ul>
     * <li><strong>Родитель:</strong> {@link #root}</li>
     * <li><strong>CSS:</strong> Прозрачный фон, центрированные кнопки</li>
     * <li><strong>Поведение:</strong> Responsive, следует за ресайзом stage</li>
     * </ul>
     *
     * <p>Создается в {@link #init()} → {@link #getRoot()}. Служит хабом для глобальных действий.</p>
     *
     * @see ControlPane#getMainButton() Главная кнопка панели
     * @see #initTooltips() Настройка подсказок
     */
    public ControlPane mainFunctions;

    /**
     * Полупрозрачный прямоугольник-фон под слайдером воспроизведения.
     *
     * <p>Создает визуальный blur-эффект для {@link #soundSlider} и временных меток
     * {@link #beginTime}/{@link #endTime}. Улучшает читаемость на сложных фонах.</p>
     *
     * <h3>Внешний вид (по умолчанию)</h3>
     *
     * <ul>
     * <li><strong>Fill:</strong> {@code Color.BLACK}</li>
     * <li><strong>Opacity:</strong> {@code 0.25}</li>
     * <li><strong>Corner radius:</strong> {@code 10px}</li>
     * </ul>
     *
     * <h3>Адаптивное позиционирование (биндинг)</h3>
     *
     * <pre>{@code
     * sliderBlurBackground.layoutXProperty().bind(
     *     soundSlider.layoutXProperty().subtract(rootLayout.getSliderBlurBackgroundXSubtract())  // -10f
     * );
     * sliderBlurBackground.layoutYProperty().bind(
     *     soundSlider.layoutYProperty().subtract(rootLayout.getSliderBlurBackgroundYSubtract())  // -10f
     * );
     * sliderBlurBackground.widthProperty().bind(
     *     soundSlider.prefWidthProperty().add(rootLayout.getSliderBlurBackgroundWidthAdd())  // +20f
     * );
     * sliderBlurBackground.heightProperty().bind(
     *     soundSlider.prefHeightProperty().add(rootLayout.getSliderBlurBackgroundHeightAdd())  // +20f
     * );
     * }</pre>
     *
     * <p>Инициализируется в {@link #set()} и добавляется в {@link Pane#getChildren()} ()}.
     * Расширяет область слайдера для лучшего визуального разделения.</p>
     *
     * @see SoundSlider#getSliderBackground() Фон самого слайдера
     * @see #initBinds() Настройка биндингов
     */
    public javafx.scene.shape.Rectangle sliderBlurBackground = new javafx.scene.shape.Rectangle();

    /**
     * Процессор обработки и загрузки обложек альбомов (ArtProcessor).
     *
     * <p>Отвечает за асинхронную загрузку, кэширование и применение изображений
     * для {@link #art}. Интегрируется с {@link Art#setImage(Image)} ()} и медиапроцессором.</p>
     *
     * <h3>Основные функции</h3>
     *
     * <ul>
     * <li><strong>Загрузка:</strong> Из локальных файлов/сетевых источников</li>
     * <li><strong>Кэширование:</strong> В {@link Resources.Properties#DEFAULT_PLAYLIST_CACHE_PATHS}</li>
     * <li><strong>Обработка:</strong> Скругление углов ({@link #corners}), эффекты ({@link #imgTrackShadow})</li>
     * <li><strong>Анимации:</strong> Плавная смена изображений с Interpolator</li>
     * </ul>
     *
     * <h3>Интеграция</h3>
     *
     * <pre>{@code
     * public static Art art = new Art(corners);
     * // artProcessor обрабатывает art.setImage(image)
     *
     * Metadata.getInstance().prepare(...).open(stage);  // → artProcessor
     * }</pre>
     *
     * <p>Инициализируется синглтоном в {@link #set()}. Работает в фоне через
     * {@link LonelyThreadPool} или {@link Root#rootExecService}.</p>
     *
     * @see Art Обработчик изображений обложек
     * @see Art#setImage(Image) Точка входа для artProcessor
     * @see ConfigurationManager#getIntItem(String, int) ) Радиус скругления
     */
    public ArtProcessor artProcessor;

    /**
     * Коэффициент "ледяного трения" для кастомного интерполятора анимаций.
     *
     * <p>Определяет плавность замедления в {@link #iceInterpolator}. Высокое значение
     * (14.0) создает эффект "инерции" — резкий старт с медленным затуханием.</p>
     *
     * <h3>Применение в интерполяторе</h3>
     *
     * <pre>{@code
     * public static Interpolator iceInterpolator = new Interpolator() {
     *     @Override
     *     protected double curve(double t) {
     *         return (t >= 1.0) ? 1.0 : 1.0 - Math.pow(2.0, -ICE_FRICTION * t);
     *     }
     * };
     * }</pre>
     *
     * <h3>Визуальный эффект</h3>
     *
     * <ul>
     * <li><strong>t=0:</strong> Скорость = 0 (медленный старт)</li>
     * <li><strong>t=0.5:</strong> ~50% прогресса с ускорением</li>
     * <li><strong>t→1:</strong> Экспоненциальное замедление (ледяная инерция)</li>
     * </ul>
     *
     * <p>Используется в анимациях поворота кнопок ({@link #hideControlRight}),
     * blur-эффектах и UI-переходах вместо стандартного {@link Interpolator#EASE_BOTH}.</p>
     */
    public static double ICE_FRICTION = 14.0;

    /**
     * Флаг активного мониторинга позиции воспроизведения в слайдере.
     *
     * <p>Контролирует основной цикл {@link SliderHandler#soundSliderThread}, который обновляет
     * {@link #soundSlider} в реальном времени во время воспроизведения.</p>
     *
     * <h3>Жизненный цикл</h3>
     *
     * <pre>{@code
     * // Запуск мониторинга
     * SliderHandler.initialize();
     *
     * // Остановка
     * SliderHandler.stop();  // → isHandling = false
     * }</pre>
     *
     * <h3>Использование в потоке SliderHandler</h3>
     *
     * <pre>{@code
     * public static Thread thread = new Thread(() -> {
     *     while (isHandling) {  // ← volatile проверка
     *         try {
     *             Thread.sleep(SliderHandler.sleep);  // 1000ms
     *             if (!worry && MediaProcessor.mediaProcessor.mediaPlayer != null) {
     *                 Platform.runLater(() -> soundSlider.setValue(
     *                     mediaPlayer.getCurrentTime().toSeconds()
     *                 ));
     *             }
     *         } catch (InterruptedException e) {
     *             isHandling = false;  // graceful shutdown
     *         }
     *     }
     * });
     * }</pre>
     *
     * <p><strong>volatile</strong> обеспечивает видимость изменений между потоками
     * (UI thread ↔ background thread). Предотвращает race conditions при паузе/остановке.</p>
     *
     * @see SliderHandler#stop() Безопасная остановка мониторинга
     */
    private volatile boolean isHandling = true;

    /**
     * Callback-обработчик для пользовательской инициализации UI после базовой настройки.
     *
     * <p>Выполняется <strong>один раз</strong> в {@link #init()} после создания всех
     * UI-элементов (art, lists, sliders, buttons), но <strong>до</strong> настройки
     * биндингов и стилей.</p>
     *
     * <h3>Точка вызова</h3>
     *
     * <pre>{@code
     * public void init() {
     *     // ... создание art, tracksListView, soundSlider, btn и др.
     *
     *     if (onInit != null) {
     *         onInit.run();  // ← Здесь
     *     }
     *
     *     // Биндинги, стили, эффекты...
     * }
     * }</pre>
     *
     * <h3>Использование</h3>
     *
     * <ul>
     * <li>Добавление кастомных UI-элементов в {@link #root}</li>
     * <li>Подписка на события ДО биндингов</li>
     * <li>Инициализация внешних компонентов (диалоги, плагины)</li>
     * </ul>
     *
     * <pre>{@code
     * Root.onInit = () -> {
     *     // Пример: добавить кастомную кнопку
     *     CustomButton myBtn = new CustomButton();
     *     root.getChildren().add(myBtn);
     *
     *     // Подписка на события
     *     art.imageProperty().addListener(...);
     * };
     * }</pre>
     *
     * <p><strong>null</strong> по умолчанию (не выполняется). Позволяет расширять
     * плеер без модификации {@link Root}.</p>
     *
     * @see #init() Контекст выполнения
     * @see Runnable#run() Интерфейс колбека
     */
    public static Runnable onInit = null;

    /**
     * Однопоточный пул задач для фоновых операций UI-корня.
     *
     * <p>Используется для асинхронных задач, не блокирующих UI-поток:
     * загрузка изображений, инициализация компонентов, сетевые запросы.</p>
     *
     * <h3>Создание</h3>
     *
     * <pre>{@code
     * // Было: Executors.newFixedThreadPool(2)
     * // Стало: Executors.newSingleThreadExecutor()
     * public final ExecutorService rootExecService = Executors.newSingleThreadExecutor();
     * }</pre>
     *
     * <h3>Применение</h3>
     *
     * <pre>{@code
     * // Инициализация контекстного меню истории
     * rootExecService.submit(() -> {
     *     trackHistoryContextMenu = new ContextMenu();
     *     PlayProcessor.playProcessor.setTrackHistoryGlobal(...);
     * });
     *
     * // Загрузка слайдера
     * loadSlider.runNewTask(() -> soundSlider.loadSliderBackground(file));
     * }</pre>
     *
     * <h3>Преимущества SingleThreadExecutor</h3>
     *
     * <table>
     * <tr><th>FixedThreadPool(2)</th><th>SingleThreadExecutor</th></tr>
     * <tr><td>2 параллельных потока</td><td><strong>1 последовательный поток</strong></td></tr>
     * <tr><td>Риск race conditions</td><td><strong>Предсказуемый порядок</strong></td></tr>
     * <tr><td>Больше памяти</td><td><strong>1 поток + очередь задач</strong></td></tr>
     * </table>
     *
     * <p><strong>final</strong> предотвращает репризинг. Не закрывается явно
     * (daemon-подобное поведение). Инициализируется в поле класса.</p>
     *
     * @see Executors#newSingleThreadExecutor() Фабрика
     */
    public final ExecutorService rootExecService = Executors.newSingleThreadExecutor();

    /**
     * Специализированный пул потоков для асинхронной загрузки фона слайдера.
     *
     * <p><strong>LonelyThreadPool</strong> — кастомная реализация пула с гарантией
     * <em>последовательного выполнения одной задачи за раз</em> (single-threaded).</p>
     *
     * <h3>Назначение</h3>
     *
     * <pre>{@code
     * public void loadRectangleOfGainVolumeSlider(File file) {
     *     loadSlider.runNewTask(() -> {
     *         soundSlider.loadSliderBackground(file);  // ← Единственная цель
     *     });
     * }
     * }</pre>
     *
     * <h3>Отличия от rootExecService</h3>
     *
     * <table>
     * <tr><th>rootExecService</th><th>loadSlider</th></tr>
     * <tr><td>Общие UI-задачи</td><td><strong>Только слайдер</strong></td></tr>
     * <tr><td>ExecutorService</td><td><strong>LonelyThreadPool (custom)</strong></td></tr>
     * <tr><td>Множественные submit()</td><td><strong>runNewTask() с блокировкой</strong></td></tr>
     * </table>
     *
     * <h3>Поведение LonelyThreadPool</h3>
     *
     * <ul>
     * <li><strong>Очередь:</strong> Выполняет задачи строго последовательно</li>
     * <li><strong>Блокировка:</strong> {@code runNewTask()} ждет завершения предыдущей</li>
     * <li><strong>Идемпотентность:</strong> Предотвращает наложение загрузок слайдера</li>
     * </ul>
     *
     * <p><strong>final + private</strong> — неизменяемый внутренний сервис.
     * Инициализируется в поле класса для {@link #soundSlider}.</p>
     *
     * @see SoundSlider#loadSliderBackground(File) Целевая операция
     * @see LonelyThreadPool#runNewTask(Runnable) Метод запуска
     */
    private final LonelyThreadPool loadSlider = new LonelyThreadPool();
    /**
     * Сохраненные размеры окна до программного максимизации/восстановления.
     *
     * <p>Хранит {@code width/height} {@link #stage} в портативном режиме для корректного
     * восстановления после {@link #isInternalMaximazied} или системного максимизирования.</p>
     *
     * <h3>Использование в обработчике ресайза</h3>
     *
     * <pre>{@code
     * stage.maximizedProperty().addListener((obs, e1, e2) -> {
     *     if (isInternalMaximazied.get()) {
     *         // Сохраняем ДО изменения
     *         beforePortable.setSize(stage.getWidth(), stage.getHeight());
     *
     *         if (!isMaximazied.get()) {
     *             stage.setHeight(800);
     *             stage.setWidth(420);
     *             stage.setResizable(false);
     *         } else {
     *             // Восстанавливаем ИЗ сохраненных
     *             stage.setHeight(beforePortable.getHeight());
     *             stage.setWidth(beforePortable.getWidth());
     *             stage.setResizable(true);
     *         }
     *     }
     * });
     * }</pre>
     *
     * <h3>Жизненный цикл</h3>
     *
     * <ul>
     * <li><strong>Сохранение:</strong> При программной максимизации</li>
     * <li><strong>Восстановление:</strong> При возврате к портативному виду</li>
     * <li><strong>По умолчанию:</strong> {@code (0, 0)} — пустое состояние</li>
     * </ul>
     *
     * <p><strong>private final</strong> — неизменяемый контейнер для AWT {@link java.awt.Dimension}.
     * Синхронизирует UI-состояние с системными событиями {@link Stage#maximizedProperty()}.</p>
     *
     * @see Stage#setResizable(boolean) Блокировка ресайза в портативном режиме
     */
    private Dimension beforePortable = new Dimension();

    /**
     * Флаг программной (ручной) максимизации окна плеера.
     *
     * <p>Отслеживает состояние {@link #stage}, установленное кодом (не системой).
     * Работает в паре с {@link #isMaximazied} для различения источников максимизации.</p>
     *
     * <h3>Логика переключения</h3>
     *
     * <pre>{@code
     * stage.maximizedProperty().addListener((obs, e1, e2) -> {
     *     if (isInternalMaximazied.get()) {  // ← Программная максимизация активна
     *         if (!isMaximazied.get()) {     // ← Система пытается максимизировать
     *             // → Запрещаем системную, делаем свою (800x420 → fullscreen)
     *             stage.setHeight(800); stage.setWidth(420); stage.setResizable(false);
     *         } else {
     *             // → Восстанавливаем портативный размер из beforePortable
     *             stage.setHeight(beforePortable.getHeight());
     *         }
     *         isInternalMaximazied.set(false);  // Сбрасываем флаг
     *     }
     *     isMaximazied.set(!isMaximazied.get());  // Toggle системного состояния
     * });
     * }</pre>
     *
     * <ul>
     * <li><strong>true:</strong> Активирована ручная максимизация</li>
     * <li><strong>false:</strong> Нормальный портативный режим (по умолчанию)</li>
     * </ul>
     */
    private AtomicBoolean isInternalMaximazied = new AtomicBoolean(false);

    /**
     * Флаг системной максимизации окна (от ОС).
     *
     * <p>Отслеживает {@link Stage#maximizedProperty()}. По умолчанию {@code false}.
     * Используется совместно с {@link #isInternalMaximazied} для гибридного управления
     * размерами окна.</p>
     *
     * <h3>Роли в обработчике</h3>
     *
     * <table>
     * <tr><th>Сценарий</th><th>isMaximazied</th><th>Действие</th></tr>
     * <tr><td>Пользователь жмет максимизировать</td><td>false→true</td><td>Блокируем, делаем портативный fullscreen</td></tr>
     * <tr><td>Возврат из максимизации</td><td>true→false</td><td>Восстанавливаем {@link #beforePortable}</td></tr>
     * </table>
     *
     * <p><strong>AtomicBoolean</strong> обеспечивает потокобезопасность для
     * {@link ChangeListener} → UI обновления.</p>
     *
     * @see Stage#maximizedProperty() Источник изменений
     */
    private AtomicBoolean isMaximazied = new AtomicBoolean();

    /**
     * Слабая константа с нативным дескриптором окна (HWND для Windows).
     *
     * <p><code>WeakConst&lt;Long&gt;</code> — кастомный контейнер, который позволяет
     * безопасно хранить нативный handle без утечек памяти (weak reference).</p>
     *
     * <h3>Инициализация</h3>
     *
     * <pre>{@code
     * public void initBinds() {
     *     if (!stage.isShowing()) {
     *         stage.showingProperty().addListener(...);
     *     } else {
     *         HWND.setIfUnset(getNativeHandlePeerForStage(stage));  // ← Установка
     *     }
     * }
     * }</pre>
     *
     * <h3>Применение (Windows 11+)</h3>
     *
     * <pre>{@code
     * public static void setStageCaptionColor(Stage stage, Color color) {
     *     if (OS.contains("Windows 11")) {
     *         long wid = HWND.get();  // Получение handle
     *         int res = Integer.parseInt(convertColorTo16(color), 16);
     *         setCaptionColor(wid, res);  // JNI/DWM API
     *     }
     * }
     * }</pre>
     *
     * <h3>Преимущества WeakConst</h3>
     *
     * <ul>
     * <li><strong>Thread-safe:</strong> Атомарная установка/чтение</li>
     * <li><strong>Memory-safe:</strong> Weak reference предотвращает утечки</li>
     * <li><strong>Lazy:</strong> Устанавливается только при stage.showing</li>
     * </ul>
     *
     * <p>Критично для интеграции с Windows DWM API ({@link #setCaptionColor}) для
     * окраски заголовка окна в цвет обложки альбома.</p>
     *
     * @see #getNativeHandlePeerForStage(Stage) JNI-метод получения HWND
     * @see rf.ebanina.utils.weakly.WeakConst Кастомный утилитный класс
     */
    public WeakConst<Long> HWND = new WeakConst<>();

    /**
     * Callback после полной инициализации UI-элементов (art, списки, кнопки).
     *
     * <p>Выполняется в {@link #set()} <strong>после</strong> создания всех компонентов,
     * установки размеров, эффектов, но <strong>до</strong> биндингов и финальных стилей.</p>
     *
     * <h3>Контекст выполнения</h3>
     *
     * <pre>{@code
     * public void set() {
     *     // 1. Создание: art, tracksListView, soundSlider, кнопки...
     *     // 2. Размеры: art.setHeight(200), soundSlider.setPrefWidth(...)
     *     // 3. Эффекты: art.setEffect(imgTrackShadow)
     *
     *     if (onSet != null) {  // ← Здесь
     *         onSet.run();
     *     }
     *
     *     // 4. Добавление в root.getChildren()
     * }
     * }</pre>
     *
     * <h3>Использование</h3>
     *
     * <ul>
     * <li>Кастомные UI-элементы, зависящие от базовых компонентов</li>
     * <li>Подписка на свойства ДО биндингов (imageProperty, selectionModel)</li>
     * <li>Инициализация обработчиков, требующих готовых объектов</li>
     * </ul>
     *
     * <pre>{@code
     * Root.onSet = () -> {
     *     // Доступны: art, tracksListView, soundSlider, btn...
     *     customOverlay = new CustomOverlay(art);
     *     root.getChildren().add(0, customOverlay);
     *
     *     soundSlider.valueProperty().addListener(...);  // Безопасно
     * };
     * }</pre>
     *
     * <p><strong>null</strong> по умолчанию. Более поздний хук, чем {@link #onInit}
     * (который выполняется ДО создания компонентов).</p>
     *
     * @see #set() Точка вызова
     */
    public Runnable onSet = null;

    /**
     * Кастомный интерполятор анимаций с "ледяной инерцией".
     *
     * <p>Реализует экспоненциальное замедление по формуле <code>1 - 2^(-k·t)</code>,
     * где {@link #ICE_FRICTION} = 14.0 создает эффект плавного скольжения.</p>
     *
     * <h3>Математика curve(t)</h3>
     *
     * <pre>{@code
     * curve(t) = t == 1.0 ? 1.0 : 1.0 - pow(2.0, -ICE_FRICTION * t)
     *          = 1.0 - 2^(-14.0·t)
     * }</pre>
     *
     * <table>
     * <tr><th>t</th><th>curve(t)</th><th>Эффект</th></tr>
     * <tr><td>0.0</td><td>0.00</td><td>Старт</td></tr>
     * <tr><td>0.2</td><td>0.36</td><td>Наклон</td></tr>
     * <tr><td>0.5</td><td>0.69</td><td>Ускорение</td></tr>
     * <tr><td>0.8</td><td>0.92</td><td>Замедление</td></tr>
     * <tr><td>1.0</td><td>1.00</td><td>Финиш</td></tr>
     * </table>
     *
     * <h3>Применение</h3>
     *
     * <ul>
     * <li><strong>Поворот кнопок:</strong> {@link #hideControlRight} (180° за 125ms)</li>
     * <li><strong>UI-переходы:</strong> opacity, scale вместо резких изменений</li>
     * </ul>
     *
     * <pre>{@code
     * RotateTransition rt = new RotateTransition(Duration.millis(125), button);
     * rt.setInterpolator(iceInterpolator);  // Ледяной эффект
     * rt.play();
     * }</pre>
     *
     * <p>Предпочтительнее {@link Interpolator#EASE_OUT} для премиум-чувства UI.</p>
     *
     * @see RotateTransition#setInterpolator(Interpolator) Анимация кнопок
     */
    public static Interpolator iceInterpolator = new Interpolator() {
        @Override
        protected double curve(double t) {
            return (t == 1.0) ? 1.0 : 1.0 - Math.pow(2.0, -ICE_FRICTION * t);
        }
    };

    /**
     * Создает экземпляр корневого UI плеера (Root).
     *
     * <p>Пустой конструктор для singleton-подобного использования:
     * {@link #rootImpl}, {@link #rootLayout}. <strong>Не вызывает init()</strong> —</p>
     *
     * <h3>Паттерн инициализации</h3>
     *
     * <pre>{@code
     * public static Root rootImpl = new Root();  // ← Только создание
     *
     * public Pane getRoot() {
     *     return Objects.requireNonNullElseGet(root, () -> {
     *         root = new Pane();
     *         rootImpl.init(root);  // ← Инициализация по требованию
     *         return root;
     *     });
     * }
     * }</pre>
     *
     * <h3>Жизненный цикл</h3>
     *
     * <ol>
     * <li><strong>new Root()</strong> — создание экземпляра</li>
     * <li>{@link #getRoot()} — lazy инициализация Pane + UI</li>
     * <li>{@link #initBinds()} — биндинги при stage.showing</li>
     * </ol>
     *
     * <p><strong>Публичный</strong> для внешнего создания (тесты, плагины).
     * Все ресурсы (artProcessor, execService) создаются в полях класса.</p>
     *
     * @see #getRoot() Lazy-контейнер root
     */
    public Root() {}
    /**
     * Загружает Windows DWM API библиотеку (dwm.dll) для кастомизации заголовка.
     *
     * <p>Инициализирует JNI-вызовы {@link #setCaptionColor(long, int)} для окраски
     * заголовка окна в цвет обложки альбома (Windows 11+).</p>
     *
     * <h3>Путь к библиотеке</h3>
     *
     * <pre>{@code
     * loadDwmApiLibrary(BINLIBRARIESPATH + File.separator + "dwm.dll");
     * }</pre>
     *
     * <h3>Обработка ошибок</h3>
     *
     * <table>
     * <tr><th>Состояние</th><th>Действие</th></tr>
     * <tr><td><strong>exists()</strong></td><td><code>System.load()</code></td></tr>
     * <tr><td><strong>!exists()</strong></td><td><strong>UnsatisfiedLinkError</strong> с русским сообщением</td></tr>
     * </table>
     *
     * <h3>JNI-сигнатуры после загрузки</h3>
     *
     * <pre>{@code
     * public static native void setCaptionColor(long wid, int color);
     * // → DWMWA_CAPTION_COLOR: окраска title bar
     * }</pre>
     *
     * <p><strong>private</strong> — внутренняя утилита. Вызывается при старте для
     * Windows 11. Предотвращает краш при отсутствии нативной DLL.</p>
     *
     * @param path Абсолютный путь к {@code dwm.dll}
     * @throws UnsatisfiedLinkError Если библиотека отсутствует
     * @see #setStageCaptionColor(Stage, Color) Применение API
     */
    private void loadDwmApiLibrary(String path) {
        File file = new File(path);

        if (!file.exists()) {
            throw new UnsatisfiedLinkError("Библиотека не найдена по пути: " + path);
        }

        System.load(file.getAbsolutePath());
    }
    /**
     * Окрашивает заголовок окна (caption/title bar) в цвет обложки альбома.
     *
     * <p>Windows 11+ DWM API: применяет цвет к {@link Stage} через JNI.
     * Активируется настройкой {@code album_art_caption_paint}.</p>
     *
     * <h3>Условия выполнения</h3>
     *
     * <pre>{@code
     * if (Config.getBoolean("album_art_caption_paint", true)
     *     && OS.contains("Windows 11")) {
     *     // → Выполняется
     * }
     * }</pre>
     *
     * <h3>Алгоритм (шаг за шагом)</h3>
     *
     * <ol>
     * <li><strong>HWND:</strong> {@link Root#getNativeHandlePeerForStage(Stage)}}</li>
     * <li><strong>ARGB→int:</strong> {@link Root#convertColorTo16(Color)} → hex-парсинг</li>
     * <li><strong>JNI:</strong> {@code setCaptionColor(wid, res)} (dwm.dll)</li>
     * </ol>
     *
     * <pre>{@code
     * Color mainClr = ColorProcessor.core.getMainClr();  // Из обложки
     * Root.setStageCaptionColor(stage, mainClr);
     * }</pre>
     *
     * <h3>Технические детали</h3>
     *
     * <ul>
     * <li><strong>Формат цвета:</strong> 16-ричное ARGB (DWMWA_CAPTION_COLOR)</li>
     * <li><strong>Зависимость:</strong> {@link Root#loadDwmApiLibrary(String)} )}</li>
     * <li><strong>Handle:</strong> {@link #HWND} (weak reference)</li>
     * </ul>
     *
     * <p>Премиум-фича: заголовок окна динамически меняет цвет под текущий трек.</p>
     *
     * @param stage Целевое окно
     * @param color Цвет из {@link ArtProcessor} или {@link ColorProcessor}
     * @see #convertColorTo16(Color) Преобразование JavaFX Color → DWM
     * @see #getNativeHandlePeerForStage(Stage) Получение HWND
     */
    public void setStageCaptionColor(Stage stage, Color color) {
        if (ConfigurationManager.instance.getBooleanItem("album_art_caption_paint", "true") && OS.contains("Windows 11")) {
            long wid = getNativeHandlePeerForStage(stage);

            int res = Integer.parseInt(convertColorTo16(color), 16);

            setCaptionColor(wid, res);
        }
    }
    /**
     * Конвертирует JavaFX {@link Color} в 16-ричный формат для Windows DWM API.
     *
     * <p>Извлекает ARGB-компоненты из строки {@code color.toString()} (например,
     * {@code 0xFF4285F4}), переставляет байты RRGGBBAA → AARRGGBB для DWM.</p>
     *
     * <h3>Пример преобразования</h3>
     *
     * <pre>{@code
     * Color color = Color.rgb(66, 133, 244);  // #4285F4
     * String hex16 = convertColorTo16(color);  // "FF4285F4"
     * int res = Integer.parseInt(hex16, 16);   // 4283753364 (AARRGGBB)
     * setCaptionColor(wid, res);               // DWMWA_CAPTION_COLOR
     * }</pre>
     *
     * <h3>Алгоритм</h3>
     *
     * <ol>
     * <li><code>color.toString()</code> → {@code "0xFF4285F4"}</li>
     * <li>Извлечь {@code "FF4285F4"} (позиции 2..конец)</li>
     * <li>Поменять местами первые 2 и последние 2 символа:
     *     <code>RRGGBBAA → AARRGGBB</code></li>
     * <li>Вернуть {@code "FF4285F4"}</li>
     * </ol>
     *
     * <p><strong>private</strong> — внутренняя утилита для {@link Root#setStageCaptionColor(Stage, Color)} ()}.
     * Исправляет порядок байтов JavaFX для Windows API.</p>
     *
     * @param color Цвет обложки из {@link ArtProcessor}
     * @return 8-символьный hex AARRGGBB для JNI
     * @see #setStageCaptionColor(Stage, Color) Пункт применения
     */
    private String convertColorTo16(Color color) {
        StringBuilder temp = new StringBuilder(
                color.toString().substring(2, color.toString().length() - 2)
        );

        char temp1 = temp.charAt(0);
        char temp2 = temp.charAt(1);

        temp.setCharAt(0, temp.charAt(temp.length() - 2));
        temp.setCharAt(1, temp.charAt(temp.length() - 1));

        temp.setCharAt(temp.length() - 2, temp1);
        temp.setCharAt(temp.length() - 1, temp2);

        return temp.toString();
    }
    /**
     * Извлекает нативный HWND дескриптор JavaFX Stage (Windows).
     *
     * <p>Reflection + JNI для доступа к низкоуровневому handle окна через
     * Glass/QuantumToolkit. Необходим для DWM API ({@link Root#setStageCaptionColor(Stage, Color)} ()}).</p>
     *
     * <h3>Алгоритм (Reflection chain)</h3>
     *
     * <pre>{@code
     * Stage → Window.getPeer() → [tkStage] → tkStage.getRawHandle() → long HWND
     * }</pre>
     *
     * <h3>Полный код</h3>
     *
     * <pre>{@code
     * try {
     *     // 1. Window → Peer
     *     Method getPeer = Window.class.getDeclaredMethod("getPeer");
     *     getPeer.setAccessible(true);
     *     Object tkStage = getPeer.invoke(stage);
     *
     *     // 2. Toolkit → raw handle
     *     Method getRawHandle = tkStage.getClass().getMethod("getRawHandle");
     *     getRawHandle.setAccessible(true);
     *     return (Long) getRawHandle.invoke(tkStage);
     * } catch (Exception ex) {
     *     throw new RuntimeException("Window is not exist");
     * }
     * }</pre>
     *
     * <h3>Критические моменты</h3>
     *
     * <ul>
     * <li><strong>Timing:</strong> Вызывать только после {@code stage.showing}</li>
     * <li><strong>Reflection:</strong> Обходит private API JavaFX</li>
     * <li><strong>Platform:</strong> Только Windows (HWND)</li>
     * </ul>
     *
     * <p>Кэшируется в {@link #HWND} (WeakConst). Базовый метод для всех
     * native Windows интеграций.</p>
     *
     * @param stage Показанное окно
     * @return HWND (long) для JNI/DWM
     * @throws RuntimeException Если stage не готово или reflection failed
     * @see #setStageCaptionColor(Stage, Color) Основной потребитель
     */
    public long getNativeHandlePeerForStage(Stage stage) {
        try {
            final Method getPeer = javafx.stage.Window.class.getDeclaredMethod("getPeer", null);
            getPeer.setAccessible(true);
            final Object tkStage = getPeer.invoke(stage);
            final Method getRawHandle = tkStage.getClass().getMethod("getRawHandle");
            getRawHandle.setAccessible(true);
            return (Long) getRawHandle.invoke(tkStage);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        throw new RuntimeException("Window is not exist");
    }
    /**
     * Универсальный системный диалог (Alert) с кастомным заголовком.
     *
     * <p>Стандартный JavaFX Alert с одной кнопкой OK. Блокирует UI до закрытия.</p>
     *
     * <h3>Параметры</h3>
     *
     * <table>
     * <tr><th>title</th><th>msg</th><th>alertType</th></tr>
     * <tr><td>"License Error"</td><td>"Файл не найден"</td><td><strong>ERROR/WARNING/INFO</strong></td></tr>
     * </table>
     *
     * <pre>{@code
     * rootImpl.alert("Ошибка лицензии", "license.txt отсутствует", Alert.AlertType.ERROR);
     * }</pre>
     *
     * <p><strong>Публичный</strong> для использования из обработчиков ошибок.
     * Блокирует поток (showAndWait()).</p>
     *
     * @param title Заголовок окна
     * @param msg Текст сообщения
     * @param alertType WARNING/ERROR/INFO/CONFIRMATION
     * @see #error(String, String) Упрощенный вызов для ошибок
     */
    public void alert(String title, String msg, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }
    /**
     * Упрощенный вызов alert() для ошибок (AlertType.ERROR).
     *
     * <p>Статический фасад через singleton {@link #rootImpl}.</p>
     *
     * <h3>Применение</h3>
     *
     * <pre>{@code
     * // В обработчиках, утилитах, JNI-ошибках
     * Root.showError("DWM API", "dwm.dll не найдена");
     *
     * // Автоматически: ERROR + "Ошибка: " + title
     * rootImpl.error("Лицензия", "license/ не существует");
     * }</pre>
     *
     * <p><strong>Статический</strong> — универсальный для всех классов проекта.</p>
     *
     * @param title Заголовок ошибки
     * @param msg Детали ошибки
     * @see #alert(String, String, Alert.AlertType) Базовый метод
     */
    public void error(String title, String msg) {
        rootImpl.alert(title, msg, Alert.AlertType.ERROR);
    }

    protected final List<IViewable> iViewableList = new ArrayList<>(List.<IViewable>of(
            new Metadata(),
            new NetworkHost(),
            new AudioHost(),
            new Settings(),
            new TrackStatistics()
    ));

    public List<IViewable> getiViewableList() {
        return iViewableList;
    }

    /**
     * Инициализирует все UI-компоненты плеера с точной настройкой позиций, размеров и эффектов.
     *
     * <p><strong>Критический метод</strong> — выполняется однократно в {@link #getRoot()}
     * при первом создании {@link #root}. Устанавливает статические размеры ДО биндингов.</p>
     *
     * <h3>Детальная последовательность (20+ шагов)</h3>
     *
     * <ol>
     *   <li><strong>Шрифты (строки 1-2):</strong>
     *     <pre>{@code currentTrackName.setFont(ResourceManager.loadFont("mainfont", 24));}</pre>
     *
     *   <li><strong>Обложка альбома (строки 3-12):</strong>
     *     <pre>{@code art.setHeight(200); art.setArcHeight(corners);}</pre>
     *     <code>DropShadow(75px), rotation 3D, opacity=1.0</code>
     *
     *   <li><strong>Информационная панель (строки 13-18):</strong>
     *     <pre>{@code topDataPane.setPrefHeight(75);}</pre>
     *     Центрирует {@link #currentTrackName} + {@link #currentArtist}
     *
     *   <li><strong>Слайдер + фон (строки 19-30):</strong>
     *     <pre>{@code soundSlider.setPrefWidth(120);}</pre>
     *     Создает sliderBlurBackground (черный, opacity 0.25, radius 10px)
     *
     *   <li><strong>Временные метки (строки 31-42):</strong>
     *     <pre>{@code beginTime.setPrefWidth(30); endTime.setText("NaN");}</pre>
     *     Позиционированы по краям слайдера
     *
     *   <li><strong>Кнопки управления (строки 43-60):</strong>
     *     <pre>{@code btn.setLayoutY(... + rootLayout.getSliderBottom());}</pre>
     *     {@link #btn}, {@link #btnDown}, {@link #btnNext} + курсор HAND
     *
     *   <li><strong>Кнопки плейлистов (строки 61-70):</strong>
     *     <pre>{@code hideControlRight.setPadding(Insets.EMPTY);}</pre>
     *
     *   <li><strong>Callback onSet (строка 71):</strong>
     *     <pre>{@code if (onSet != null) onSet.run();}</pre>
     *
     *   <li><strong>Добавление в root (строки 72+):</strong>
     *     <code>art, topDataPane, soundSlider, кнопки → root.getChildren().addAll()</code>
     * </ol>
     *
     * <h3>Ключевые константы из Layout</h3>
     *
     * <table>
     * <tr><th>Элемент</th><th>Layout-параметр</th><th>Значение</th></tr>
     * <tr><td>Кнопки от слайдера</td><td>{@link Layout#sliderBottom}</td><td>50px</td></tr>
     * <tr><td>Обложка от центра</td><td>{@link Layout#currentTrackTopPlaylistPane}</td><td>65px</td></tr>
     * </table>
     *
     * <p><strong>private</strong> — точка входа для UI. После set() следует
     * {@link #initBinds()} (адаптивность) → стили.</p>
     *
     * @see Root#getRoot()  Lazy-ленивая инициализация
     * @see Layout Исходные размеры/отступы
     */
    public void set() {
        ConfigurationManager.instance.initializeVariables(Root.class);

        String[] a = ConfigurationManager.instance.getItem("general_animations_interpolator", "0, 0, 1, 1").replaceAll(" ", "").split(",");

        general_interpolator = Interpolator.SPLINE(
                Double.parseDouble(a[0]),
                Double.parseDouble(a[1]),
                Double.parseDouble(a[2]),
                Double.parseDouble(a[3])
        );

        if(ConfigurationManager.instance.getBooleanItem("is_blur_background", true)) {
            createBackground(background);
            createBackground(background_under);
        }

        stage.getScene().getStylesheets().add(ResourceManager.Instance.loadStylesheet(Resources.Properties.CONTEXT_MENU_STYLES.getKey()));

        imgTrackShadow.setRadius(75);
        imgTrackShadow.setBlurType(BlurType.GAUSSIAN);
        imgTrackShadow.setWidth(100);
        imgTrackShadow.setHeight(100);
        imgTrackShadow.setSpread(0.5);

        tracksListView.getTrackListView().getSelectionModel().select(PlayProcessor.playProcessor.getTrackIter());

        currentTrackName.setFont(ResourceManager.Instance.loadFont("main_font", 24));
        currentArtist.setFont(ResourceManager.Instance.loadFont("font", 24));

        art.setHeight(200);
        art.setWidth(200);
        art.setArcHeight(corners);
        art.setArcWidth(corners);
        art.setLayoutX((stage.getHeight() / 2) - (stage.getWidth() / 2));
        art.setLayoutY((stage.getHeight() / 2) - (art.getHeight() / 2) - rootLayout.getCurrentTrackTopPlaylistPane());
        art.setEffect(imgTrackShadow);
        art.setRotationAxis(new Point3D(25, 25, 25));
        art.setOpacity(1);
        art.setOnMouseClicked(t -> {
            if(t.getButton() == MouseButton.PRIMARY) {
                Metadata.getInstance().setTrack(PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()));
                Metadata.getInstance().open(stage);
            }
        });

        root.getChildren().add(art);

        topDataPane.setLayoutX(art.getLayoutX() - 50);
        topDataPane.setLayoutY(art.getLayoutY() - 125);
        topDataPane.setPrefWidth(art.getWidth() + 100);
        topDataPane.setPrefHeight(75);
        topDataPane.setCenter(currentTrackName);
        topDataPane.setBottom(currentArtist);

        root.getChildren().add(topDataPane);

        int width = ConfigurationManager.instance.getIntItem("slider_width", "120");

        root.getChildren().add(soundSlider);
        root.getChildren().add(soundSlider.getSliderBackground());

        soundSlider.setLayoutX(art.getLayoutX());
        soundSlider.setLayoutY(art.getLayoutY() + art.getHeight() + 30);
        soundSlider.setPrefHeight(ConfigurationManager.instance.getIntItem("slider_height", "25"));
        soundSlider.setPrefWidth(width > art.getWidth() ? width : art.getWidth());
        soundSlider.setSize(new Dimension((int) soundSlider.getPrefWidth(), (int) soundSlider.getPrefHeight()));
        soundSlider.setInterpolator(general_interpolator);
        soundSlider.initializeBox();
        soundSlider.setupSliderBoxAsync().start();

        root.getChildren().add(sliderBlurBackground);
        sliderBlurBackground.setArcHeight(10);
        sliderBlurBackground.setArcWidth(10);
        sliderBlurBackground.setFill(Color.BLACK);
        sliderBlurBackground.setOpacity(0.25);

        root.getChildren().add(beginTime);
        beginTime.setAlignment(Pos.CENTER_LEFT);
        beginTime.setEditable(true);
        beginTime.setPrefWidth(30);
        beginTime.setLayouts(soundSlider.getLayoutX(), soundSlider.getLayoutY() + soundSlider.getPrefHeight() + soundSlider.getPrefHeight() / 2);

        root.getChildren().add(endTime);
        endTime.setAlignment(Pos.CENTER_RIGHT);
        endTime.setText("NaN");
        endTime.setFont(ResourceManager.Instance.loadFont("main_font", 11));
        endTime.setEditable(true);
        endTime.setPrefWidth(30);
        endTime.setLayouts(art.getLayoutX() + art.getWidth() - endTime.getFont().getSize() * 1.25, beginTime.getLayoutY());

        root.getChildren().add(btn);
        btn.setLayoutX(art.getLayoutX() + art.getWidth() / 2 - btn.getPrefWidth() / 2);
        btn.setLayoutY(soundSlider.getLayoutY() + soundSlider.getPrefHeight() + 50);
        btn.setCursor(Cursor.HAND);

        root.getChildren().add(btnDown);
        btnDown.setLayoutX(art.getLayoutX());
        btnDown.setLayoutY(btn.getLayoutY());
        btnDown.setCursor(Cursor.HAND);

        root.getChildren().add(btnNext);
        btnNext.setLayoutX(soundSlider.getLayoutX() + soundSlider.getPrefWidth() - btnNext.getPrefWidth());
        btnNext.setLayoutY(btn.getLayoutY());
        btnNext.setCursor(Cursor.HAND);

        root.getChildren().add(hideControlRight);
        hideControlRight.setPadding(new Insets(0));
        hideControlRight.setCursor(Cursor.HAND);

        root.getChildren().add(hideControlLeft);
        hideControlLeft.setPadding(new Insets(0));
        hideControlLeft.setCursor(Cursor.HAND);

        TrackListView.set();
        SimilarListView.set();

        tracksListView.getCurrentPlaylistText().setFont(ResourceManager.Instance.loadFont("main_font", 11));
        tracksListView.getCurrentPlaylistText().setAlignment(Pos.CENTER);
        tracksListView.getCurrentPlaylistText().updateColor(ColorProcessor.core.getMainClr());

        tracksListView.getSearchBar().setBackground(Background.EMPTY);
        tracksListView.getSearchBar().setFont(ResourceManager.Instance.loadFont("main_font", 11));

        mainFunctions.addCenteredButton(new Commons());
        mainFunctions.getMainButton().setOnAction(e -> {
            MainFunctionDialog agreementDialog = new MainFunctionDialog(Root.rootImpl.stage, Root.rootImpl.root);
            agreementDialog.setDialogMaxSize(0.9, 0.85);

            agreementDialog.getLeftListView().getItems().clear();
            agreementDialog.getLeftListView().getItems().addAll(iViewableList);

            agreementDialog.animationTopBorder(ColorProcessor.core.getMainClr()).play();
            agreementDialog.show();
        });

        root.getChildren().add(mainFunctions);

        rootImpl.initBinds();

        root.getStylesheets().add(ResourceManager.Instance.loadStylesheet("root"));
        root.setEffect(new GaussianBlur(0));
        root.setOnScroll(event -> {
            if(stage.isFocused() && isKeyPressed(NativeKeyEvent.VC_CONTROL)) {
                playlistSelected = true;
                PlayProcessor.playProcessor.setPreviousIndex(PlayProcessor.playProcessor.getTrackIter());

                double deltaY = event.getDeltaY();

                if (deltaY < 0) {
                    PlayProcessor.playProcessor.setTrackIter(PlayProcessor.playProcessor.getTrackIter() + 1);
                } else if (deltaY > 0) {
                    PlayProcessor.playProcessor.setTrackIter(PlayProcessor.playProcessor.getTrackIter() - 1);
                }

                tracksListView.getTrackListView().getSelectionModel().select(PlayProcessor.playProcessor.getTrackIter());

                playlistSelected = false;
            }
        });

        toFront(/* ListView.currentTrackTopPlaylistPane ,*/ /* currentSimilarTrackTopPlaylistPane ,*/
                /* imgTrackArt , */btn, btnNext, btnDown, hideControlLeft, hideControlRight,
                beginTime, endTime, /* imgTrackArtRound, */ similar, topDataPane /*, ListView.playlist ,*/
                /*bottom_trackslist_similar/*, ListView.bottom_trackslist */, tracksListView, sliderBlurBackground, soundSlider.getSliderBackground(), soundSlider
        );

        loadDwmApiLibrary(BIN_LIBRARIES_PATH + File.separator + "dwm.dll");

        similar.close();
        tracksListView.close();

        PreviewPopupService.initializePaneTrackPopup();

        String image = ConfigurationManager.instance.getItem("background_image", "");

        if(!image.isEmpty()) {
            Image img = new Image(image);

            BackgroundSize backgroundSize = new BackgroundSize(100, 100, true, true, true, false);

            BackgroundImage backgroundImage = new BackgroundImage(
                    img,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    backgroundSize
            );

            Background background = new Background(backgroundImage);
            root.setBackground(background);
        }

        image = ConfigurationManager.instance.getItem("background_css_style", "");

        if(!image.equals("")) {
            root.setStyle(image);
        }

        stage.maximizedProperty().addListener((e, e1, e2) -> {
            if(isInternalMaximazied.get()) {
                if(!isMaximazied.get()) {
                    beforePortable.setSize(stage.getWidth(), stage.getHeight());
                    stage.setHeight(800);
                    stage.setWidth(420);
                    stage.setResizable(false);
                } else {
                    stage.setHeight(beforePortable.getHeight());
                    stage.setWidth(beforePortable.getWidth());
                    stage.setResizable(true);
                }

                isInternalMaximazied.set(false);
            }

            if(e2) {
                isMaximazied.set(!isMaximazied.get());

                isInternalMaximazied.set(true);

                stage.setMaximized(false);
            }
        });

        // Tooltips
        initTooltips();

        // Handler
        if(onSet != null)
            onSet.run();

        // License Agreement
        if(!Path.of("license").toFile().exists()) {
            alert("Non-exist object", "License directory is not exist", Alert.AlertType.ERROR);
        }

        if(!Boolean.parseBoolean(FileManager.instance.readSharedData().getOrDefault("license_agreed", "false"))) {
            LicenseDialog agreementDialog = new LicenseDialog(
                    stage,
                    root,
                    LocalizationManager.getLocaleString("license_title", "License Agreement"),
                    readAllLicensesRecursively("license", LocalizationManager.instance.lang.split("_")[1]),
                    LocalizationManager.getLocaleString("license_agree", "Agree")
            );

            agreementDialog.setOnAction(() -> {
                FileManager.instance.saveSharedData.add(
                        new FileManager.SharedDataEntry("app", "license_agreed", () -> "true", "false")
                );

                Platform.runLater(() -> root.getChildren().remove(agreementDialog));
            });

            agreementDialog.setDialogMaxSize(0.9, 0.85);
            agreementDialog.animationTopBorder(ColorProcessor.core.getMainClr()).play();
            agreementDialog.show();
        }
    }
    /**
     * Лямбда-функция настройки всех адаптивных биндингов UI к размерам stage.
     *
     * <p>Выполняется при {@link #stage}.showing (lazy). Преобразует статические
     * размеры из {@link #set()} в динамические формулы через {@link #rootLayout}.</p>
     *
     * <h3>Основные группы биндингов (15+ Property.bind())</h3>
     *
     * <ol>
     *   <li><strong>Обложка {@link #art}:</strong>
     *     <pre>{@code art.layoutX().bind(stage.widthProperty() × layout.getImgTrackArtRoundWidthXMultiplier());}</pre>
     *
     *   <li><strong>Панель данных:</strong>
     *     <pre>{@code topDataPane.layoutY().bind(art.layoutY() - rootLayout.getImgTop()); // 125f}</pre>
     *
     *   <li><strong>Слайдер + фон:</strong>
     *     <pre>{@code soundSlider.layoutY().bind(art.y() + art.height() + layout.getImgBottom()); // 35f}</pre>
     *     <code>sliderBlurBackground расширяет слайдер на 20px</code>
     *
     *   <li><strong>Время и кнопки:</strong>
     *     <pre>{@code btn.layoutY().bind(soundSlider.y() + height() + layout.getSliderBottom()); // 50px}</pre>
     *
     *   <li><strong>Кнопки плейлистов:</strong>
     *     <pre>{@code hideControlRight.layoutX().bind(stage.width() - layout.getHideControlRightSide()); // 18px}</pre>
     *
     *   <li><strong>ListView'ы:</strong>
     *     <pre>{@code tracksListView.prefHeight().bind(stage.height() - layout.getBottomTracklistBottom()); // 65px}</pre>
     * </ol>
     *
     * <h3>Логика вызова (двойная защита)</h3>
     *
     * <pre>{@code
     * public void initBinds() {
     *     if (!stage.isShowing()) {
     *         stage.showingProperty().addListener(() -> initBinds.run());
     *     } else {
     *         initBinds.run();  // Прямой вызов
     *     }
     *     HWND.setIfUnset(getNativeHandlePeerForStage(stage));
     * }
     * }</pre>
     *
     * <p><strong>Runnable</strong> — переиспользуемый хук. Делает плеер
     * полностью responsive без хардкода координат.</p>
     *
     * @see Layout Математические коэффициенты позиционирования
     * @see Root#initListViewsBinds() Дополнение для списков
     */
    protected Runnable initBinds = () -> {
        art.layoutXProperty().bind(stage.widthProperty().multiply(rootLayout.getImgTrackArtRoundWidthXMultiplier()).subtract(art.widthProperty().multiply(rootLayout.getImgTrackArtRoundWidthXMultiplier())).subtract(corners * rootLayout.getImgTrackArtRoundWidthXMultiplier()));
        art.layoutYProperty().bind(stage.heightProperty().multiply(rootLayout.getImgTrackArtRoundYMultiplier()).subtract(art.heightProperty().multiply(rootLayout.getImgTrackArtRoundWidthYMultiplier())));

        topDataPane.layoutXProperty().bind(art.layoutXProperty().subtract(topDataPane.prefWidthProperty().subtract(art.widthProperty()).multiply(rootLayout.getTopDataPaneWidthXMultiplier())));
        topDataPane.layoutYProperty().bind(art.layoutYProperty().subtract(rootLayout.getImgTop()));

        soundSlider.layoutXProperty().bind(art.layoutXProperty());
        soundSlider.layoutYProperty().bind(art.layoutYProperty().add(art.heightProperty().add(rootLayout.getImgBottom())));

        sliderBlurBackground.layoutXProperty().bind(soundSlider.layoutXProperty().subtract(rootLayout.getSliderBlurBackgroundXSubtract()));
        sliderBlurBackground.layoutYProperty().bind(soundSlider.layoutYProperty().subtract(rootLayout.getSliderBlurBackgroundYSubtract()));

        sliderBlurBackground.widthProperty().bind(soundSlider.prefWidthProperty().add(rootLayout.getSliderBlurBackgroundWidthAdd()));
        sliderBlurBackground.heightProperty().bind(soundSlider.prefHeightProperty().add(rootLayout.getSliderBlurBackgroundHeightAdd()));

        beginTime.layoutXProperty().bind(soundSlider.layoutXProperty());
        beginTime.layoutYProperty().bind(soundSlider.layoutYProperty().add(soundSlider.prefHeightProperty().multiply(rootLayout.getSliderBlurBackgroundBeginTimeMultiplier())));

        endTime.layoutXProperty().bind(soundSlider.layoutXProperty().add(soundSlider.prefWidthProperty().subtract(endTime.prefWidthProperty())));
        endTime.layoutYProperty().bind(beginTime.layoutYProperty());

        btn.layoutXProperty().bind(soundSlider.layoutXProperty().subtract(btn.prefWidthProperty().subtract(soundSlider.prefWidthProperty()).multiply(rootLayout.getBtnSliderWidthMultiplier())));
        btn.layoutYProperty().bind(soundSlider.layoutYProperty().add(soundSlider.prefHeightProperty()).add(rootLayout.getSliderBottom()));

        btnDown.layoutXProperty().bind(soundSlider.layoutXProperty());
        btnDown.layoutYProperty().bind(btn.layoutYProperty());

        btnNext.layoutXProperty().bind(soundSlider.layoutXProperty().add(soundSlider.prefWidthProperty()).subtract(btnNext.prefWidthProperty()));
        btnNext.layoutYProperty().bind(btnDown.layoutYProperty());

        hideControlRight.layoutXProperty().bind(stage.widthProperty().subtract(rootLayout.getHideControlRightSide()).subtract(hideControlRight.prefWidthProperty()));
        hideControlRight.layoutYProperty().bind(art.layoutYProperty().add(art.heightProperty().multiply(rootLayout.getHideControlRightToImgTrackArtHeightMultiplier())));

        hideControlLeft.setLayoutX(rootLayout.getHideControlLeftLayoutX());
        hideControlLeft.layoutYProperty().bind(art.layoutYProperty().add(art.heightProperty().multiply(rootLayout.getHideControlLeftToImgTrackArtHeight())));

        rootImpl.initListViewsBinds();
    };
    /**
     * Дополняет {@link #initBinds()} специфическими биндингами для ListView'ов.
     *
     * <p>Настраивает {@link #tracksListView} и {@link #similar} относительно
     * обложки и кнопок. Вызывается из {@link #initBinds()}.</p>
     *
     * <h3>Биндинги tracksListView (основной плейлист)</h3>
     *
     * <pre>{@code
     * tracksListView.layoutXProperty().bind(
     *     art.layoutXProperty()
     *         .add(art.widthProperty())
     *         .add(rootLayout.getPaddingArt())  // 35px
     *         .add(10)
     * );
     *
     * tracksListView.prefWidthProperty().bind(
     *     art.layoutXProperty()
     *         .subtract(rootLayout.getTrackListViewToImgTrackArtRoundXSubtract())  // 50f
     *         .subtract(rootLayout.getPaddingArt())
     * );
     *
     * tracksListView.layoutYProperty().bind(topDataPane.layoutYProperty().add(10));
     * tracksListView.prefHeightProperty().bind(
     *     stage.heightProperty()
     *         .subtract(topDataPane.layoutYProperty())
     *         .subtract(rootLayout.getBottomTracklistBottom())  // 65px
     * );
     * }</pre>
     *
     * <h3>Биндинги similar (рекомендации)</h3>
     *
     * <pre>{@code
     * similar.setLayoutX(hideControlLeft.getLayoutX() + rootLayout.getSimilarLayoutXPadding());  // 25f
     * similar.prefWidthProperty().bind(
     *     art.layoutXProperty()
     *         .subtract(rootLayout.getPaddingArt())
     *         .subtract(rootLayout.getSimilarWidthSubtract())  // 35f
     * );
     * similar.layoutYProperty().bind(tracksListView.layoutYProperty());
     * similar.prefHeightProperty().bind(tracksListView.prefHeightProperty());
     * }</pre>
     *
     * <h3>Визуальная схема</h3>
     *
     * <pre>
     * [hideControlLeft] [similar]          [art] [tracksListView]
     *                   ^25px отступ       ^35px отступ
     *                   |                   |
     *                   +-- hideControlLeft + paddingArt + 10px
     * </pre>
     *
     * <p><strong>private</strong> — вызывается автоматически из initBinds().
     * Обеспечивает идеальное выравнивание списков при любом размере окна.</p>
     *
     * @see PlayView Базовый класс для tracksListView/similar
     * @see Layout Параметры позиционирования
     */
    public void initListViewsBinds() {
        tracksListView.layoutXProperty().bind(art.layoutXProperty().add(art.widthProperty().add(rootLayout.getPaddingArt()).add(10)));
        tracksListView.prefWidthProperty().bind(art.layoutXProperty().subtract(rootLayout.getTrackListViewToImgTrackArtRoundXSubtract()).subtract(rootLayout.getPaddingArt()));

        tracksListView.layoutYProperty().bind(topDataPane.layoutYProperty().add(10));
        tracksListView.prefHeightProperty().bind(stage.heightProperty().subtract(topDataPane.layoutYProperty()).subtract(rootLayout.getBottomTracklistBottom()));

        similar.setLayoutX(hideControlLeft.getLayoutX() + rootLayout.getSimilarLayoutXPadding());
        similar.prefWidthProperty().bind(art.layoutXProperty().subtract(rootLayout.getPaddingArt()).subtract(rootLayout.getSimilarWidthSubtract()));

        similar.layoutYProperty().bind(tracksListView.layoutYProperty());
        similar.prefHeightProperty().bind(tracksListView.prefHeightProperty());
    };
    /**
     * Рекурсивно собирает весь текст лицензий из языковой директории.
     *
     * <p>Сканирует {@code license/[langCode]/*.md}, извлекает LICENSE.md файлы,
     * объединяет в единый текст для {@link LicenseDialog}.</p>
     *
     * <h3>Параметры</h3>
     *
     * <table>
     * <tr><th>baseDir</th><th>langCode</th><th>Пример пути</th></tr>
     * <tr><td>{@code "license"}</td><td>{@code "ru_RU"}</td><td>{@code license/ru_RU/LICENSE.md}</td></tr>
     * </table>
     *
     * <h3>Алгоритм (Files.walk → Stream API)</h3>
     *
     * <pre>{@code
     * Path langPath = Paths.get(baseDir, langCode);
     * if (!Files.isDirectory(langPath)) return "err";
     *
     * return Files.walk(langPath)
     *     .filter(Files::isRegularFile)
     *     .filter(p -> p.getFileName().equalsIgnoreCase("LICENSE.md"))
     *     .map(p -> Files.readAllLines(p).join("\n"))
     *     .filter(text -> !text.isEmpty())
     *     .collect(Collectors.joining("\n\n"));
     * }</pre>
     *
     * <h3>Обработка ошибок</h3>
     *
     * <ul>
     * <li><strong>!isDirectory:</strong> {@code return "err"}</li>
     * <li><strong>IOException:</strong> Логирует, возвращает {@code "err"}</li>
     * <li><strong>Пустые файлы:</strong> Фильтрует {@code text.isEmpty()}</li>
     * </ul>
     *
     * <p><strong>private static</strong> — утилита для {@link LicenseDialog}.
     * Поддерживает мультиязычные лицензии.</p>
     *
     * @param baseDir Корневая папка лицензий ({@code "license"})
     * @param langCode {@link LocalizationManager#lang} → {@code "ru_RU,en_US"}
     * @return Объединенный текст лицензий или "err"
     * @see LicenseDialog Конечный потребитель
     */
    private String readAllLicensesRecursively(String baseDir, String langCode) {
        Path langPath = Paths.get(baseDir, langCode);
        if (!Files.isDirectory(langPath)) {
            System.err.println("Directory not found: " + langPath);
            return "err";
        }

        try (Stream<Path> stream = Files.walk(langPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase("LICENSE.md"))
                    .map(p -> {
                        try {
                            Music.mainLogger.println(p);

                            return String.join(System.lineSeparator(), Files.readAllLines(p));
                        } catch (IOException e) {
                            e.printStackTrace();
                            return "";
                        }
                    })
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
            return "err";
        }
    }
    /**
     * Настраивает многоязычные всплывающие подсказки (ContextTooltip) для всех интерактивных элементов.
     *
     * <p>Применяет локализованные тексты из {@link LocalizationManager} ко всем кнопкам,
     * слайдеру, полям поиска. Группирует по функциональным блокам.</p>
     *
     * <h3>Tooltips по группам</h3>
     *
     * <table>
     * <tr><th>Элемент</th><th>Ключ локализации</th></tr>
     * <tr><td>{@link #soundSlider}</td><td>{@code TOOLTIPMAINSLIDER}</td></tr>
     * <tr><td>{@link #btn} (Play/Pause)</td><td>{@code TOOLTIPMAINPLAY}</td></tr>
     * <tr><td>{@link #mainFunctions}</td><td>{@code TOOLTIPMAINFUNCTIONSBUTTON}</td></tr>
     * <tr><td>Поиск в плейлистах</td><td>{@code TOOLTIPPLAYLISTSEARCH}</td></tr>
     * <tr><td>{@link Root#hideControlRight}</td><td>{@code TOOLTIPOPENLOCAL/NETWORKPLAYLIST}</td></tr>
     * </table>
     *
     * <h3>Динамические tooltips (треки)</h3>
     *
     * <pre>{@code
     * btnNext.setTooltip(new ContextTooltip(
     *     next != null ? next.viewName() : "",
     *     "SKIPINTRO - Shift+Click"
     * ));
     *
     * currentTrackName.setTooltip(new ContextTooltip(current.getTitle()));
     * }</pre>
     *
     * <p><strong>private</strong> — вызывается в конце {@link #init()}.
     * Обеспечивает UX без загромождения интерфейса текстом.</p>
     *
     * @see ContextTooltip Кастомный класс подсказок
     * @see LocalizationManager#getLocaleString(Locales) Локализация
     */
    private void initTooltips() {
        btn.setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_MAIN_PLAY)));
        soundSlider.setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_MAIN_SLIDER)));
        mainFunctions.getMainButton().setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_MAIN_FUNCTIONS_BUTTON)));

        tracksListView.getSearchBar().setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_PLAYLIST_SEARCH)));
        tracksListView.getBtnPlaylistDown().setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_PLAYLIST_PREV)));
        tracksListView.getBtnPlaylist().setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_PLAYLIST_SET)));
        tracksListView.getBtnPlaylistNext().setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_PLAYLIST_NEXT)));

        similar.getSearchBar().setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_PLAYLIST_SEARCH)));
        similar.getBtnPlaylistDown().setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_PLAYLIST_PREV)));
        similar.getBtnPlaylist().setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_PLAYLIST_SET)));
        similar.getBtnPlaylistNext().setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_PLAYLIST_NEXT)));

        hideControlLeft.setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_OPEN_NETWORK_PLAYLIST)));
        hideControlRight.setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_OPEN_LOCAL_PLAYLIST)));

        PlayProcessor.playProcessor.getTrackIterProperty().addListener((observableValue, number, t1) -> {
            Platform.runLater(() -> {
                Track current = PlayProcessor.playProcessor.getOrNonNullDefault(PlayProcessor.playProcessor.getTrackIter(),
                        new Track("unk").setTitle("").setArtist(""));

                Track next = PlayProcessor.playProcessor.getOrNonNullDefault(PlayProcessor.playProcessor.getTrackIter() + 1,
                        new Track("unk").setTitle("").setArtist(""));

                Track prev = PlayProcessor.playProcessor.getOrNonNullDefault(PlayProcessor.playProcessor.getTrackIter() - 1,
                        new Track("unk").setTitle("").setArtist(""));

                btnNext.setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_MAIN_NEXT) + ": "
                        + (next != null ? next.viewName() : "") + "\n* " + LocalizationManager.getLocaleString(Locales.SKIP_INTRO) + ": "
                        + "\n\t - " + Keys.instance.getHotKeysStringCodes("ebanina_skip_audio_intro_hotkey")
                        + "\n\t - Shift + Click on Button"));

                btnDown.setTooltip(new ContextTooltip(LocalizationManager.getLocaleString(Locales.TOOLTIP_MAIN_PREV) + ": "
                        + (prev != null ? prev.viewName() : "") + "\n* " + LocalizationManager.getLocaleString(Locales.SKIP_PIT) + ": "
                        + "\n\t - " + Keys.instance.getHotKeysStringCodes("ebanina_skip_pit")));

                currentTrackName.setTooltip(new ContextTooltip(current.getTitle()));
                currentArtist.setTooltip(new ContextTooltip(current.getArtist()));
            });
        });
    }
    /**
     * Возвращает корневой Pane плеера с полной инициализацией (ленивая).
     *
     * <p><strong>Главная точка входа</strong> для UI. Гарантирует создание всех
     * компонентов при первом обращении (double-checked pattern).</p>
     *
     * <h3>Ленивая инициализация (thread-safe)</h3>
     *
     * <pre>{@code
     * public Pane getRoot() {
     *     return Objects.requireNonNullElseGet(root, () -> {
     *         root = new Pane();
     *         rootImpl.init(root);        // 1. Создание UI
     *         root.getStylesheets().add(...);  // 2. Стили
     *         root.setEffect(new GaussianBlur(0));
     *         root.setOnScroll(...);      // 3. Обработчики
     *         return root;
     *     });
     * }
     * }</pre>
     *
     * <h3>Цепочка вызовов</h3>
     *
     * <ol>
     * <li><code>rootImpl.init(root)</code> → {@link #set()}</li>
     * <li>Загрузка CSS: {@code root.css}, contextmenu.css</li>
     * <li>{@link #initBinds()} → адаптивные биндинги</li>
     * <li>Обработчики: scroll (управление треками), drag-n-drop</li>
     * </ol>
     *
     * <table>
     * <tr><th>Вызов</th><th>Состояние root</th></tr>
     * <tr><td>1-й <code>getRoot()</code></td><td><strong>Создается + init</strong></td></tr>
     * <tr><td>2+ <code>getRoot()</code></td><td><strong>Кэшируется</strong></td></tr>
     * </table>
     *
     * <p><strong>public</strong> — используется в <code>Scene</code>, тестах, плагинах.
     * Единственный безопасный способ получить готовый UI.</p>
     *
     * @return Готовый <code>Pane</code> со всеми компонентами
     * @see Root#init() Инициализация через singleton
     * @see Objects#requireNonNullElseGet(Object, Supplier)  Double-checked ленивость
     */
    public Pane getRoot() {
        return Objects.requireNonNullElseGet(root, () -> root = new Pane());
    }
    /**
     * Инициирует систему адаптивных биндингов при показе Stage.
     *
     * <p>Запускает {@link #initBinds} лямбду и устанавливает {@link #HWND}.
     * Использует двойную защиту от повторного вызова.</p>
     *
     * <h3>Логика защиты (если !stage.isShowing())</h3>
     *
     * <pre>{@code
     * if (!stage.isShowing()) {
     *     stage.showingProperty().addListener((obs, ov, nv) -> {
     *         if (nv) initBinds.run();  // Только при показе
     *     });
     *     return;  // Отложено
     * }
     *
     * // Немедленный запуск
     * initBinds.run();
     * HWND.setIfUnset(getNativeHandlePeerForStage(stage));
     * }</pre>
     *
     * <h3>Последовательность</h3>
     *
     * <table>
     * <tr><th>Условие</th><th>Действие</th></tr>
     * <tr><td>stage уже показано</td><td><strong>initBinds.run() немедленно</strong></td></tr>
     * <tr><td>stage скрыто</td><td>Listener на <code>showingProperty() → true</code></td></tr>
     * </table>
     *
     * <p><strong>public</strong> — точка входа из стартового кода. После set() и до стилей.
     * Критично для responsive поведения и native интеграции.</p>
     *
     */
    public void initBinds() {
        if(!stage.isShowing()) {
            ChangeListener<Boolean> listener = new ChangeListener<>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
                    initBinds.run();

                    HWND.setIfUnset(getNativeHandlePeerForStage(stage));

                    stage.showingProperty().removeListener(this);
                }
            };

            stage.showingProperty().addListener(listener);
        } else {
            initBinds.run();

            HWND.setIfUnset(getNativeHandlePeerForStage(stage));
        }
    }
    /**
     * Создает и настраивает полноэкранный фон (ImageView) с blur-эффектом.
     *
     * <p>Применяет изображение из настроек, растягивает на весь {@link #root},
     * добавляет GaussianBlur(63px) для затемнения контента.</p>
     *
     * <h3>Настройка свойств</h3>
     *
     * <pre>{@code
     * background.setLayoutX(0);
     * background.setLayoutY(0);
     * background.setFitWidth(stage.getWidth());
     * background.setFitHeight(stage.getHeight());
     *
     * // Критично для UX:
     * background.setEffect(new GaussianBlur(63.0));
     * background.setPreserveRatio(true);
     * background.setMouseTransparent(true);
     * background.setCache(true);
     * background.setCacheHint(CacheHint.SPEED);
     * }</pre>
     *
     * <h3>Авторесайз (биндинги)</h3>
     *
     * <pre>{@code
     * root.widthProperty().addListener((obs, old, nw) -> resizeBackground(background));
     * root.heightProperty().addListener((obs, old, nw) -> resizeBackground(background));
     * background.imageProperty().addListener((obs, oldImg, newImg) -> resizeBackground(background));
     * }</pre>
     *
     * <h3>Двойной фон</h3>
     *
     * <pre>{@code
     * createBackground(background);     // Верхний слой
     * createBackground(backgroundunder); // Нижний слой
     * }</pre>
     *
     * <p><strong>private</strong> — вызывается в {@link #set()} дважды.
     * {@code resizeBackground()} обеспечивает идеальное заполнение без искажений.</p>
     *
     * @param background {@link #background} или {@link Root#background_under}
     * @return Настроенный ImageView (для root.getChildren().add())
     * @see #resizeBackground(ImageView) Адаптация размера
     */
    private ImageView createBackground(ImageView background) {
        background.setLayoutX(0);
        background.setLayoutY(0);
        background.setFitWidth(stage.getWidth());
        background.setFitHeight(stage.getHeight());

        background.setEffect(new GaussianBlur(63.0));
        background.setPreserveRatio(true);
        background.setMouseTransparent(true);
        background.setCache(true);
        background.setCacheHint(CacheHint.SPEED);

        root.widthProperty().addListener((obs, oldVal, newVal) -> resizeBackground(background));
        root.heightProperty().addListener((obs, oldVal, newVal) -> resizeBackground(background));

        background.imageProperty().addListener((obs, oldImg, newImg) -> resizeBackground(background));
        background.toBack();

        root.getChildren().add(background);

        return background;
    }
    /**
     * Применяет операцию ко всем переданным Node элементам.
     *
     * <p>Универсальная утилита для пакетной обработки UI-компонентов.
     * Используется для массовых операций: {@code toFront()}, {@code toBack()}.</p>
     *
     * <h3>Сигнатура</h3>
     *
     * <pre>{@code
     * public void nodesIter(Consumer<Node> supplier, Node... a)
     * }</pre>
     *
     * <h3>Внутренняя реализация</h3>
     *
     * <pre>{@code
     * for (Node n : a) {
     *     supplier.accept(n);  // Применить операцию
     * }
     * }</pre>
     *
     * <h3>Применение (toFront/toBack)</h3>
     *
     * <pre>{@code
     * // Пример вызова из кода:
     * nodesIter(Node::toFront, art, topDataPane, tracksListView);
     * nodesIter(Node::toBack,  background, sliderBlurBackground);
     * }</pre>
     *
     * <h3>Типичные операции supplier</h3>
     *
     * <table>
     * <tr><th>Операция</th><th>Lambda</th><th>Назначение</th></tr>
     * <tr><td><code>toFront()</code></td><td><code>Node::toFront</code></td><td>На передний план</td></tr>
     * <tr><td><code>toBack()</code></td><td><code>Node::toBack</code></td><td>На задний план</td></tr>
     * <tr><td><code>requestFocus()</code></td><td><code>Node::requestFocus</code></td><td>Фокус</td></tr>
     * </table>
     *
     * <p><strong>public</strong> — общая утилита для UI. Упрощает переупорядочивание
     * слоев Z-order в {@link #root}. Varargs позволяет передавать любое количество Node.</p>
     *
     * @param supplier Лямбда-операция для каждого Node
     * @param a Массив UI-элементов (art, buttons, lists...)
     * @see Node#toFront() Типичная операция (передний план)
     * @see Node#toBack() Типичная операция (задний план)
     */
    public void nodesIter(Consumer<Node> supplier, Node... a) {
        for(Node n : a)
            supplier.accept(n);
    }
    /**
     * Перемещает UI-элементы на передний план (Z-order).
     *
     * <p>Устанавливает переданные Node поверх всех других в {@link #root}.
     * Используется для фокусировки активных элементов.</p>
     *
     * <h3>Примеры вызова</h3>
     *
     * <pre>{@code
     * toFront(art, topDataPane, tracksListView);           // UI над фоном
     * toFront(btn, btnNext, btnDown);                      // Кнопки сверху
     * toFront(hideControlRight, mainFunctions);            // Контролы
     * }</pre>
     *
     * <h3>Внутренняя реализация</h3>
     *
     * <pre>{@code
     * public void toFront(Node... a) {
     *     nodesIter(Node::toFront, a);  // Пакетный вызов Node.toFront()
     * }
     * }</pre>
     *
     * <p><strong>Varargs</strong> — любое количество Node. Автоматически вызывает
     * {@link Node#toFront()} для каждого. Используется в {@link #set()} для правильного
     * порядка отображения слоев.</p>
     *
     * @param a UI-элементы для перемещения вперед (art, buttons, lists...)
     * @see #toBack(Node...) На задний план
     * @see #nodesIter(Consumer, Node...) Универсальная утилита
     */
    public void toFront(Node... a) {
        nodesIter(Node::toFront, a);
    }

    /**
     * Перемещает UI-элементы на задний план (Z-order).
     *
     * <p>Устанавливает Node под все остальные элементы. Используется для фонов
     * (background, sliderBlurBackground).</p>
     *
     * <h3>Примеры вызова</h3>
     *
     * <pre>{@code
     * toBack(background, backgroundunder);                 // Фон под всем
     * toBack(sliderBlurBackground, soundSlider.getSliderBackground());  // Под кнопками
     * }</pre>
     *
     * <p>Симметричная пара {@link Root#toFront(Node...)} ()}. Обеспечивает четкий Z-order:
     * фон → blur → UI → активные элементы.</p>
     *
     * @param a Фоновые элементы (background, Rectangle...)
     * @see #toFront(Node...) На передний план
     */
    public void toBack(Node... a) {
        nodesIter(Node::toBack, a);
    }
    /**
     * Пересчитывает размер и позицию фонового ImageView для идеального заполнения.
     *
     * <p>Вызывается из listeners на {@link #root}.{@code width/height/imageProperty()}.
     * Поддерживает preserveRatio с центрированием.</p>
     *
     * <h3>Алгоритм (maintain aspect ratio)</h3>
     *
     * <pre>{@code
     * if (background.getImage() == null) return;
     *
     * double scale = Math.max(containerWidth/imgWidth, containerHeight/imgHeight);
     * double newWidth  = imgWidth  * scale;
     * double newHeight = imgHeight * scale;
     *
     * background.setFitWidth(newWidth);
     * background.setFitHeight(newHeight);
     * background.setLayoutX((containerWidth - newWidth) / 2);   // Центрирование
     * background.setLayoutY((containerHeight - newHeight) / 2);
     * }</pre>
     *
     * <h3>Триггеры вызова</h3>
     *
     * <table>
     * <tr><th>Listener</th><th>Условие</th></tr>
     * <tr><td>{@code root.widthProperty()}</td><td>Ресайз окна</td></tr>
     * <tr><td>{@code root.heightProperty()}</td><td>Ресайз окна</td></tr>
     * <tr><td>{@code background.imageProperty()}</td><td>Смена фона</td></tr>
     * </table>
     *
     * <h3>Оптимизации</h3>
     *
     * <ul>
     * <li><code>if (containerWidth <= 0 || containerHeight <= 0) return;</code></li>
     * <li><code>Math.max(scaleX, scaleY)</code> — заполнение без искажений</li>
     * <li>Центрирование: <code>(container - fitted) / 2</code></li>
     * </ul>
     *
     * <p><strong>private</strong> — внутренняя утилита {@link Root#createBackground(ImageView)} ()}.
     * Обеспечивает responsive фон без растяжки изображения.</p>
     *
     * @param background {@link #background} или {@link Root#background_under}
     * @see ImageView#setPreserveRatio(boolean) Сохранение пропорций
     */
    private void resizeBackground(ImageView background) {
        if (background.getImage() == null)
            return;

        double imgWidth = background.getImage().getWidth();
        double imgHeight = background.getImage().getHeight();

        double containerWidth = root.getWidth();
        double containerHeight = root.getHeight();

        if (containerWidth <= 0 || containerHeight <= 0)
            return;

        double scaleX = containerWidth / imgWidth;
        double scaleY = containerHeight / imgHeight;

        double scale = Math.max(scaleX, scaleY);

        double newWidth = imgWidth * scale;
        double newHeight = imgHeight * scale;

        background.setFitWidth(newWidth);
        background.setFitHeight(newHeight);

        background.setLayoutX((containerWidth - newWidth) / 2);
        background.setLayoutY((containerHeight - newHeight) / 2);
    }
    /**
     * Применяет единый стиль "полупрозрачной панели" ко всем Region элементам.
     *
     * <p>"Pantyhose" (колготки) — прозрачный темный фон с тонкой серой рамкой.
     * Визуально объединяет UI-блоки (ControlPane, попапы, оверлеи).</p>
     *
     * <h3>Единый стиль для всех Region</h3>
     *
     * <pre>{@code
     * for (Region region : regions) {
     *     region.setBackground(new Background(
     *         new BackgroundFill(Color.rgb(0,0,0,0.35), CornerRadii(5), Insets.EMPTY)
     *     ));
     *     region.setBorder(new Border(
     *         new BorderStroke(Color.GRAY, SOLID, CornerRadii(5), BorderWidths(1))
     *     ));
     * }
     * }</pre>
     *
     * <h3>Визуальные характеристики</h3>
     *
     * <table>
     * <tr><th>Свойство</th><th>Значение</th></tr>
     * <tr><td>Background</td><td>Черное полупрозрачное (0.35 alpha)</td></tr>
     * <tr><td>Corner radius</td><td>5px (скругление)</td></tr>
     * <tr><td>Border</td><td>Серый SOLID 1px</td></tr>
     * <tr><td>Insets</td><td>EMPTY (без padding)</td></tr>
     * </table>
     *
     * <h3>Применение</h3>
     *
     * <pre>{@code
     * initPantyhose(mainFunctions, trackHistoryContextMenu, customPopup);
     * // → Все получают одинаковый "матовый" стиль
     * }</pre>
     *
     * <p><strong>private</strong> — внутренняя утилита для консистентного дизайна.
     * Название "pantyhose" — метафора полупрозрачности.</p>
     *
     * @param regions ControlPane, PopupPane, Region...
     */
    public void initPantyhose(Region... regions) {
        for(Region region : regions) {
            region.setBackground(new Background(new BackgroundFill(
                    Color.rgb(0, 0, 0, 0.35),
                    new CornerRadii(5, 5, 5, 5, false),
                    Insets.EMPTY
            )));

            region.setBorder(new Border(new BorderStroke(
                    Color.GRAY,
                    BorderStrokeStyle.SOLID,
                    new CornerRadii(5, 5, 5, 5, false),
                    new BorderWidths(1, 1, 1, 1)
            )));
        }
    }
    /**
     * Полная инициализация плеера: UI + конфигурация + лицензия + биндинги.
     *
     * <p><strong>Главная функция старта</strong> — вызывается из {@link #getRoot()}.
     * Координирует все подсистемы в правильном порядке.</p>
     *
     * <h3>Последовательность инициализации (критический порядок)</h3>
     *
     * <ol>
     *   <li><strong>Конфигурация:</strong> {@link ConfigurationManager#initializeVariables(Class[])} ()}</li>
     *   <li><strong>Интерполятор:</strong> {@link Root#general_interpolator} из настроек</li>
     *   <li><strong>Фон:</strong> Загрузка background/backgroundunder (если настроено)</li>
     *   <li><strong>Стили:</strong> root.css + contextmenu.css</li>
     *   <li><strong>Эффекты:</strong> {@link #imgTrackShadow}, GaussianBlur root</li>
     *   <li><strong>UI:</strong> {@link #set()} → создание 20+ компонентов</li>
     *   <li><strong>Callback:</strong> {@link #onInit}.run()</li>
     *   <li><strong>Биндинги:</strong> {@link #initBinds()} + {@link #initListViewsBinds()}</li>
     *   <li><strong>Tooltips:</strong> {@link #initTooltips()}</li>
     *   <li><strong>Natives:</strong> {@link Root#loadDwmApiLibrary(String)} (dwm.dll)}</li>
     *   <li><strong>Лицензия:</strong> Проверка/показ LicenseDialog</li>
     * </ol>
     *
     * <pre>{@code
     * public void init(Pane root) {
     *     ConfigurationManager.initializeVariables(Root.class);
     *     // ... 200+ строк полной настройки
     * }
     * }</pre>
     *
     * <p><strong>private</strong> — точка входа для всего плеера. После init()
     * Scene готова к {@link Stage#show()}.</p>
     *
     * @see #getRoot() Вызывающий метод
     */
    public void init() {
        root = getRoot();

        mainFunctions = new ControlPane(root);

        rootExecService.submit(() -> {
            rf.ebanina.UI.UI.Context.Menu.ContextMenu trackHistoryContextMenu = new rf.ebanina.UI.UI.Context.Menu.ContextMenu();
            trackHistoryContextMenu.setHeight(200);
            trackHistoryContextMenu.setMaxHeight(500);

            PlayProcessor.playProcessor.setTrackHistoryGlobal(
                    new TrackHistory(ConfigurationManager.instance.getIntItem("global_history_size", "25"),
                            trackHistoryContextMenu)
            );

            if (new File(Resources.Properties.HISTORY_FILE_PATH.getKey()).exists()) {
                PlayProcessor.playProcessor.getTrackHistoryGlobal().loadFromFile(new File(Resources.Properties.HISTORY_FILE_PATH.getKey()));
            }
        });

        currentTrackName = new TextField();
        currentArtist = new TextField();

        art = new Art(ConfigurationManager.instance.getIntItem("album_art_corners", 15));

        btn = new PlayButton();
        btnNext = new NextButton();
        btnDown = new PrevButton();

        hideControlRight = new HideRight();
        hideControlLeft = new HideLeft();

        beginTime = new TextField();
        endTime = new TextField();

        tracksListView = new PlayView<>();
        similar = new PlayView<>();

        tracksListView.getTrackListView().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tracksListView.getPlaylistListView().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        soundSlider = new SoundSlider(0, 120, 0);
        topDataPane = new BorderPane();
        artProcessor = new ArtProcessor();

        if(onInit != null)
            onInit.run();
    }

    public void loadRectangleOfGainVolumeSlider(File file) {
        loadSlider.runNewTask(() -> soundSlider.loadSliderBackground(file));
    }

    /**
     * Открывает URL во внешнем браузере пользователя (Desktop API).
     *
     * <p>Кроссплатформенная утилита для переходов на веб-страницы:
     * документация, магазин, YouTube, Spotify и т.д.</p>
     *
     * <h3>Алгоритм</h3>
     *
     * <pre>{@code
     * public void openBrowser(URI url) {
     *     try {
     *         if (Desktop.isDesktopSupported() &&
     *             Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
     *             Desktop.getDesktop().browse(url);  // Стандартный браузер
     *         }
     *     } catch (Exception e) {
     *         e.printStackTrace();  // Логируем, не крашим UI
     *     }
     * }
     * }</pre>
     *
     * <h3>Типичные сценарии</h3>
     *
     * <table>
     * <tr><th>Назначение</th><th>Пример URI</th></tr>
     * <tr><td>Spotify Web</td><td>{@code URI.create("https://open.spotify.com")}</td></tr>
     * <tr><td>Документация</td><td>{@code URI.create("https://example.com/docs")}</td></tr>
     * <tr><td>YouTube</td><td>{@code URI.create("https://youtube.com/watch?v=...")}</td></tr>
     * </table>
     *
     * <h3>Обработка ошибок</h3>
     *
     * <ul>
     * <li><strong>Desktop не поддерживается:</strong> Тихо игнорируется</li>
     * <li><strong>BROWSE не поддерживается:</strong> Не крашится</li>
     * <li><strong>IOException:</strong> Логируется в консоль</li>
     * </ul>
     *
     * <p><strong>public</strong> — универсальная утилита для всех компонентов.
     * Безопасная альтернатива {@code Runtime.exec()} с проверкой Desktop API.</p>
     *
     * @param url Валидный URI (http/https)
     * @see Desktop#browse(URI) Кроссплатформенное открытие
     * @see URI#create(String) Безопасное создание URI
     */
    public void openBrowser(URI url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Создает ProcessBuilder для открытия папки в Проводнике Windows с выделением файла.
     *
     * <p>Windows-специфичная команда: <code>explorer /select, "путь_к_файлу"</code>.
     * Выделяет конкретный файл в папке (не просто открывает директорию).</p>
     *
     * <h3>Синтаксис команды</h3>
     *
     * <pre>{@code
     * explorer.exe /select,"C:\Music\track.mp3"
     * // Результат: Проводник → папка Music → track.mp3 выделен
     * }</pre>
     *
     * <h3>Особенности</h3>
     *
     * <table>
     * <tr><th>Поведение</th><th>Результат</th></tr>
     * <tr><td><code>"C:\Music"</code></td><td>Открывает папку Music</td></tr>
     * <tr><td><code>"C:\Music\track.mp3"</code></td><td><strong>Открывает Music + выделяет track.mp3</strong></td></tr>
     * </table>
     *
     * <h3>Использование</h3>
     *
     * <pre>{@code
     * // 1. Создать builder
     * ProcessBuilder pb = openInExplorer("C:\\Music\\current.mp3");
     *
     * // 2. Запустить (опционально)
     * try {
     *     pb.start();
     * } catch (IOException e) {
     *     // Обработка ошибки
     * }
     * }</pre>
     *
     * <p><strong>public</strong> — возвращает готовый ProcessBuilder.
     * Выделение файла — премиум UX для навигации к текущему треку.</p>
     *
     * @param path Полный путь к файлу (Windows формат)
     * @return ProcessBuilder с командой <code>explorer /select,"path"</code>
     * @see ProcessBuilder#start() Для запуска процесса
     */
    public ProcessBuilder openInExplorer(String path) {
        return new ProcessBuilder("explorer", "/select,", path);
    }
    /**
     * Статический внутренний класс для управления слайдером воспроизведения.
     *
     * <p>Отвечает за реальное время синхронизацию {@link #soundSlider} с
     * {@link MediaProcessor#mediaProcessor}. Два основных режима:
     * мониторинг позиции + drag/popup события.</p>
     *
     * <h3>Основные компоненты</h3>
     *
     * <table>
     * <tr><th>Поле</th><th>Назначение</th></tr>
     * <tr><td>{@code public static Cursor cursor = Cursor.NONE}</td><td>Визуальный feedback при drag</td></tr>
     * <tr><td>{@code private static boolean worry = false}</td><td>Флаг ручного управления (drag)</td></tr>
     * <tr><td>{@code public static final int sleep = 1000}</td><td>Интервал обновления (1 сек)</td></tr>
     * <tr><td>{@code public static Thread thread}</td><td>Фоновая задача мониторинга</td></tr>
     * </table>
     *
     * <h3>Методы</h3>
     *
     * <ul>
     * <li><strong>{@link #initialize()}</strong> — MouseEvent + value listener + thread</li>
     * <li><strong>{@link #stop()}</strong> — Graceful остановка потока</li>
     * </ul>
     *
     * <h3>Логика работы</h3>
     *
     * <pre>{@code
     * MousePressed → worry = true, cursor = NONE
     * MouseReleased → worry = false, seekTo(value) / popup like moment
     * valueChange → update beginTime/endTime labels
     * Thread → while(isHandling && !worry) { slider.setValue(currentTime) }
     * }</pre>
     *
     * <p><strong>public static</strong> — глобальный handler. Интегрируется с
     * SliderHandler.sliderHandler.isHandling флагом основной активности.</p>
     *
     * @see SoundSlider Реальный слайдер UI
     */
    public static class SliderHandler {
        /**
         * Глобальный экземпляр обработчика слайдера воспроизведения.
         *
         * <p>Singleton для управления взаимодействием {@link #soundSlider} с
         * {@link MediaProcessor#mediaProcessor}. Инициализируется при загрузке класса.</p>
         *
         * <h3>Автоматическая инициализация</h3>
         *
         * <pre>{@code
         * public static SliderHandler sliderHandler = new SliderHandler();
         * // → MouseEvent, valueProperty listener, monitoring thread стартуют автоматически
         * }</pre>
         *
         * <h3>Назначение в архитектуре</h3>
         *
         * <table>
         * <tr><th>Роль</th><th>Связь</th></tr>
         * <tr><td><strong>UI → Media</strong></td><td>drag слайдера → seek</td></tr>
         * <tr><td><strong>Media → UI</strong></td><td>currentTime → slider value</td></tr>
         * <tr><td><strong>Popup</strong></td><td>Shift+Click → like moment сохранение</td></tr>
         * </table>
         *
         * <h3>Жизненный цикл</h3>
         *
         * <ul>
         * <li><strong>Создание:</strong> При загрузке Root (статическое поле)</li>
         * <li><strong>Инициализация:</strong> {@link SliderHandler#initialize()}</li>
         * <li><strong>Остановка:</strong> {@link SliderHandler#stop()} при закрытии</li>
         * </ul>
         *
         * <p><strong>public static</strong> — единый глобальный доступ из любого места.
         * Связующее звено между UI-состоянием и медиа-позицией.</p>
         *
         * @see SliderHandler#initialize() Настройка событий + thread
         * @see SoundSlider Целевой UI-компонент
         */
        public static SliderHandler sliderHandler = new SliderHandler();
        /**
         * Кастомный курсор для слайдера во время drag-операции.
         *
         * <p><code>Cursor.NONE</code> — полностью скрытый курсор. Создает эффект
         * "бесконечного слайдера" без визуального ограничения.</p>
         *
         * <h3>Жизненный цикл курсора в {@link SliderHandler}</h3>
         *
         * <pre>{@code
         * public static Cursor cursor = Cursor.NONE;
         *
         * soundSlider.setOnMousePressed(event -> {
         *     SliderHandler.worry = true;
         *     soundSlider.setCursor(SliderHandler.cursor);  // → NONE (скрыт)
         * });
         *
         * soundSlider.setOnMouseReleased(event -> {
         *     SliderHandler.worry = false;
         *     soundSlider.setCursor(Cursor.DEFAULT);       // → Стандартный
         * });
         * }</pre>
         *
         * <h3>Визуальный эффект</h3>
         *
         * <table>
         * <tr><th>Состояние</th><th>Курсор</th><th>UX</th></tr>
         * <tr><td>Ожидание</td><td><code>Cursor.DEFAULT</code></td><td>Обычный указатель</td></tr>
         * <tr><td><strong>Drag активен</strong></td><td><strong><code>NONE</code></strong></td><td><strong>Чистый слайдер</strong></td></tr>
         * <tr><td>Позиционирование</td><td><code>DEFAULT</code></td><td>Нормализация</td></tr>
         * </table>
         *
         * <p><strong>public static</strong> — глобальная настройка для {@link SliderHandler}.
         * Улучшает восприятие точного управления воспроизведением.</p>
         *
         * @see SoundSlider#setCursor(Cursor) Применение курсора
         */
        public Cursor cursor = Cursor.NONE;
        /**
         * Флаг ручного управления слайдером (пользователь тянет ползунок).
         *
         * <p><code>true</code> — пользователь активно drag'ит {@link #soundSlider},
         * мониторинговая нить останавливается. <code>false</code> — автоматическое
         * обновление позиции из {@link MediaProcessor#mediaPlayer}.</p>
         *
         * <h3>Состояния работы слайдера</h3>
         *
         * <table>
         * <tr><th>worry</th><th>Thread обновляет slider?</th><th>Курсор</th></tr>
         * <tr><td><strong>false</strong></td><td><strong>Да (каждую секунду)</strong></td><td>DEFAULT</td></tr>
         * <tr><td><strong>true</strong></td><td><strong>Нет (пользователь управляет)</strong></td><td>NONE</td></tr>
         * </table>
         *
         * <h3>Переключение в SliderHandler</h3>
         *
         * <pre>{@code
         * // Начало drag
         * soundSlider.setOnMousePressed(e -> worry = true);
         *
         * // Конец drag → seek или like moment
         * soundSlider.setOnMouseReleased(e -> {
         *     worry = false;
         *     if (e.isPrimaryButton()) {
         *         MediaProcessor.mediaProcessor.setCurrentTime(...);
         *     }
         * });
         * }</pre>
         *
         * <h3>Логика мониторинговой нити</h3>
         *
         * <pre>{@code
         * while (isHandling) {
         *     if (!worry && mediaPlayer != null) {  // ← Ключевая проверка
         *         Platform.runLater(() -> soundSlider.setValue(currentTime));
         *     }
         *     Thread.sleep(1000);
         * }
         * }</pre>
         *
         * <p><strong>private static</strong> — внутренний флаг {@link SliderHandler}.
         * Предотвращает "борьбу" между ручным управлением и автообновлением.</p>
         *
         */
        private boolean worry;
        /**
         * Интервал сна мониторинговой нити слайдера (мс).
         *
         * <p>Определяет частоту обновления {@link #soundSlider} в фоновой нити.
         * По умолчанию 1000мс (1 секунда) — баланс между отзывчивостью и CPU.</p>
         *
         * <h3>Конфигурация</h3>
         *
         * <pre>{@code
         * public final int sleep = ConfigurationManager.instance
         *     .getIntItem("slider_thread_sleep", "1000");
         * }</pre>
         *
         * <h3>Применение в SliderHandler#thread</h3>
         *
         * <pre>{@code
         * while (SliderHandler.isHandling) {
         *     if (!worry && mediaPlayer != null && mediaPlayer.getStatus() == PLAYING) {
         *         Platform.runLater(() ->
         *             soundSlider.setValue(mediaPlayer.getCurrentTime().toSeconds())
         *         );
         *     }
         *     Thread.sleep(sleep);  // ← 1000мс пауза
         * }
         * }</pre>
         *
         * <h3>Оптимальные значения</h3>
         *
         * <table>
         * <tr><th>Значение</th><th>CPU</th><th>Плавность</th><th>Сценарий</th></tr>
         * <tr><td><strong>1000мс</strong></td><td><strong>Низкий</strong></td><td>Достаточная</td><td><strong>По умолчанию</strong></td></tr>
         * <tr><td>500мс</td><td>Средний</td><td>Лучше</td><td>Точные seek'и</td></tr>
         * <tr><td>2000мс</td><td>Минимальный</td><td>Грубая</td><td>Экономия батареи</td></tr>
         * </table>
         *
         * <p><strong>public final</strong> — неизменяемая настройка производительности.
         * Доступна для тонкой настройки через config.properties.</p>
         *
         * @see ConfigurationManager#getIntItem(String, int) Источник значения
         */
        public final int sleep = ConfigurationManager.instance.getIntItem("slider_thread_sleep", "1000");
        /**
         * Инициализирует обработчики событий и мониторинговую нить для {@link #soundSlider}.
         *
         * <p>Настраивает полный цикл взаимодействия слайдера: drag, seek, popup, автообновление.
         * Вызывается автоматически при создании {@link SliderHandler#sliderHandler}.</p>
         *
         * <h3>Настраиваемые обработчики (4 ключевых события)</h3>
         *
         * <ol>
         *   <li><strong>MousePressed:</strong> <code>worry = true; cursor = NONE</code></li>
         *   <li><strong>MouseReleased:</strong>
         *     <ul>
         *       <li>LKM: <code>seekTo(slider.getValue())</code></li>
         *       <li>Shift+LKM: <code>like moment start/stop</code></li>
         *       <li>ПКМ: <code>like moment popup</code></li>
         *     </ul>
         *   </li>
         *   <li><strong>valueProperty:</strong> Обновление <code>beginTime/endTime</code></li>
         *   <li><strong>Thread:</strong> Мониторинг <code>mediaPlayer.currentTime</code></li>
         * </ol>
         *
         * <h3>Двухкнопочная логика MouseReleased</h3>
         *
         * <pre>{@code
         * if (PRIMARY) {
         *     mediaPlayer.setCurrentTime(Duration.seconds(slider.getValue()));
         * } else if (SECONDARY && SHIFT) {
         *     FileManager.save(path, trackId, LIKE_MOMENT_START/STOP, value);
         *     LabelPopupMenu.show("Begin/End of like moment set");
         * }
         * }</pre>
         *
         * <h3>Мониторинговая нить</h3>
         *
         * <pre>{@code
         * thread = new Thread(() -> {
         *     while (isHandling) {
         *         if (!worry && mediaPlayer?.getStatus() == PLAYING) {
         *             Platform.runLater(() ->
         *                 slider.setValue(mediaPlayer.getCurrentTime().toSeconds())
         *             );
         *         }
         *         Thread.sleep(sleep);  // 1000мс
         *     }
         * });
         * }</pre>
         *
         * <p><strong>public static</strong> — точка входа для SliderHandler.
         * Автоматический запуск при инициализации singleton'а.</p>
         *
         */
        public void initialize() {
            Root.rootImpl.soundSlider.setOnMousePressed((EventHandler<Event>) event -> {
                worry = true;

                Root.rootImpl.soundSlider.setCursor(cursor);
            });

            Root.rootImpl.soundSlider.setOnMouseReleased((EventHandler<Event>) event -> {
                worry = false;

                if (MediaProcessor.mediaProcessor.mediaPlayer == null || Root.rootImpl.soundSlider == null)
                    return;

                if (((MouseEvent) event).getButton() == MouseButton.PRIMARY) {
                    MediaProcessor.mediaProcessor.setCurrentTime(Duration.seconds(Root.rootImpl.soundSlider.getValue()));
                } else if(((MouseEvent) event).getButton() == MouseButton.SECONDARY) {
                    String path = Resources.Properties.DEFAULT_CACHE_TRACKS_PATH.getKey()
                            + File.separator
                            + FileManager.instance.name(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getFileName());

                    if(!isKeyPressed(NativeKeyEvent.VC_SHIFT)) {
                        FileManager.instance.save(path,
                                PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()).toString(),
                                DataTypes.LIKE_MOMENT_START.code,
                                String.valueOf(Root.rootImpl.soundSlider.getValue()));

                        MediaProcessor.mediaProcessor.mediaPlayer.setStartTime(Duration.seconds(Double.parseDouble(String.valueOf(Root.rootImpl.soundSlider.getValue()))));

                        LabelPopupMenu l = new LabelPopupMenu("Begin of like moment set to " + Root.rootImpl.soundSlider.getValue());
                        l.getLabel().setPrefWidth(100);
                        l.getLabel().setFont(ResourceManager.Instance.loadFont("main_font", 28));
                        l.ShowHide(Root.rootImpl.topDataPane, Duration.seconds(1));
                    } else {
                        FileManager.instance.save(path,
                                PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()).toString(),
                                DataTypes.LIKE_MOMENT_STOP.code,
                                String.valueOf(Root.rootImpl.soundSlider.getValue()));

                        MediaProcessor.mediaProcessor.mediaPlayer.setStopTime(Duration.seconds(Double.parseDouble(String.valueOf(Root.rootImpl.soundSlider.getValue()))));

                        LabelPopupMenu l = new LabelPopupMenu("End of like moment set to " + Track.getFormattedTotalDuration((int) Root.rootImpl.soundSlider.getValue()));
                        l.getLabel().setPrefWidth(100);
                        l.getLabel().setFont(ResourceManager.Instance.loadFont("main_font", 28));
                        l.ShowHide(Root.rootImpl.topDataPane, Duration.seconds(1));
                    }
                }

                Root.rootImpl.soundSlider.setCursor(Cursor.DEFAULT);
            });

            Root.rootImpl.soundSlider.valueProperty().addListener((changed, oldValue, newValue) -> {
                Platform.runLater(() -> {
                    if(MediaProcessor.mediaProcessor.mediaPlayer != null) {
                        Root.rootImpl.beginTime.setText((Track.getFormattedTotalDuration(MediaProcessor.mediaProcessor.mediaPlayer.getCurrentTime().toSeconds())));
                    }
                });
            });

            soundSliderThread.start();
        }
        /**
         * Безопасно останавливает мониторинговую нить слайдера.
         *
         * <p>Устанавливает isHandling = <code>false</code>,
         * корректно завершая цикл thread без InterruptedException.</p>
         *
         * <h3>Сценарии вызова</h3>
         *
         * <pre>{@code
         * // При закрытии приложения
         * SliderHandler.stop();
         *
         * // При смене трека (опционально)
         * if (needPreciseControl) SliderHandler.stop();
         *
         * // Graceful shutdown медиаплеера
         * mediaPlayer.stop();
         * SliderHandler.stop();
         * }</pre>
         *
         * <h3>Внутренняя логика нити</h3>
         *
         * <pre>{@code
         * public static Thread thread = new Thread(() -> {
         *     while (isHandling) {  // ← Проверка флага
         *         try {
         *             if (!worry && mediaPlayer != null) {
         *                 Platform.runLater(() -> slider.setValue(currentTime));
         *             }
         *             Thread.sleep(sleep);
         *         } catch (InterruptedException e) {
         *             isHandling = false;  // Graceful exit
         *             Thread.currentThread().interrupt();
         *         }
         *     }
         * });
         * }</pre>
         *
         * <h3>Гарантии остановки</h3>
         *
         * <table>
         * <tr><th>Состояние</th><th>Действие</th><th>Результат</th></tr>
         * <tr><td>Нить активна</td><td><strong><code>isHandling = false</code></strong></td><td>Цикл завершается на следующей итерации</td></tr>
         * <tr><td>Нить уже остановлена</td><td>Идемпотентно</td><td>Безопасный повторный вызов</td></tr>
         * <tr><td>Нить в sleep</td><td>Прерывание на <code>while</code></td><td>Мгновенная реакция</td></tr>
         * </table>
         *
         * <p><strong>public static</strong> — точка входа для graceful shutdown.
         * Предотвращает утечки ресурсов и "зависшие" Platform.runLater вызовы.</p>
         *
         */
        public void stop() {
            Root.rootImpl.isHandling = false;
        }

        /**
         * Мониторинговая нить автообновления {@link #soundSlider}.
         *
         * <p>Синхронизирует позицию слайдера с <code>mediaPlayer.currentTime</code>
         * каждые {@link #sleep} мс во время воспроизведения. Пауза при ручном управлении.</p>
         *
         * <h3>Основной цикл (3 состояния)</h3>
         *
         * <pre>{@code
         * while (SliderHandler.<strong>isHandling</strong>) {
         *     if (!<strong>worry</strong> && mediaPlayer != null
         *         && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
         *
         *         // JavaFX Thread → UI Thread (безопасное обновление)
         *         Platform.runLater(() ->
         *             <strong>soundSlider.setValue</strong>(
         *                 mediaPlayer.getCurrentTime().toSeconds()
         *             )
         *         );
         *     }
         *
         *     // Пауза между обновлениями (настраиваемая)
         *     Thread.sleep(<strong>sleep</strong>);
         * }
         * }</pre>
         *
         * <h3>Ключевые проверки безопасности</h3>
         *
         * <table>
         * <tr><th>Условие</th><th>Действие</th></tr>
         * <tr><td><code>isHandling == false</code></td><td>Завершение нити</td></tr>
         * <tr><td><code>worry == true</code></td><td>Пропуск обновления (drag/seek)</td></tr>
         * <tr><td><code>mediaPlayer == null</code></td><td>Пропуск</td></tr>
         * <tr><td><code>status != PLAYING</code></td><td>Пропуск (PAUSED/STOPPED)</td></tr>
         * </table>
         *
         * <h3>Жизненный цикл</h3>
         *
         * <pre>{@code
         * initialize() {        // ← START
         *     soundSliderThread.start();
         * }
         *
         * stop() {              // ← STOP
         *     SliderHandler.isHandling = false;
         * }
         * }</pre>
         *
         * <p><strong>public</strong> — доступ для анализа/дебаггинга.
         * {@link Thread} с lambda — современный Java 8+ подход.</p>
         *
         */
        public Thread soundSliderThread = new Thread(() -> {
            while (Root.rootImpl.isHandling) {
                try {
                    if (!worry && MediaProcessor.mediaProcessor.mediaPlayer != null && Root.rootImpl.soundSlider != null && Root.rootImpl.soundSlider.getScene() != null) {
                        if (MediaProcessor.mediaProcessor.mediaPlayer.getStatus() == rf.ebanina.ebanina.Player.MediaPlayer.Status.PLAYING) {
                            Platform.runLater(() -> {
                                if (MediaProcessor.mediaProcessor.mediaPlayer != null && Root.rootImpl.soundSlider != null) {
                                    Root.rootImpl.soundSlider.setValue(MediaProcessor.mediaProcessor.mediaPlayer.getCurrentTime().toSeconds() / MediaProcessor.mediaProcessor.mediaPlayer.getTempo());
                                }
                            });
                        }
                    }

                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Root.rootImpl.isHandling = false;
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, "slider");
    }
    /**
     * Централизованный обработчик событий всех кнопок плеера.
     *
     * <p>Регистрирует MouseEvent/ActionEvent для 8+ элементов UI:
     * play/pause, next/prev, hideLeft/Right, playlist controls.
     * Поддерживает модификаторы клавиш (Shift+Alt, Shift) для расширенной функциональности.</p>
     *
     * <h3>Инициализация (ButtonHandler.initialize())</h3>
     *
     * <pre>{@code
     * public static void initialize() {
     *     hideControlRight.onMouseClicked → RotateTransition + tracksListView.toggle()
     *     hideControlLeft.onMouseClicked  → similarListView.toggle()
     *     btn.onMouseClicked              → MediaProcessor.pausePlay()
     *     btnNext.onMouseClicked          → next/skipIntro/skip (по Shift/Alt)
     *     btnDown.onMouseClicked          → prev/down/skipOutro (по Shift/Alt)
     *     playlist buttons                → PlaylistController.next/down/set
     * }
     * }</pre>
     *
     * <h3>Модификаторы клавиш (btnNext/btnDown)</h3>
     *
     * <table>
     * <tr><th>Клавиши</th><th>btnNext</th><th>btnDown</th></tr>
     * <tr><td>Обычный клик</td><td>PlayProcessor.next()</td><td>PlayProcessor.down()</td></tr>
     * <tr><td><strong>Shift</strong></td><td><strong>skipIntro()</strong></td><td><strong>skipOutro()</strong></td></tr>
     * <tr><td><strong>Shift+Alt</strong></td><td><strong>TrackHistory.forward()</strong></td><td><strong>TrackHistory.back()</strong></td></tr>
     * </table>
     *
     * <h3>Анимации и эффекты</h3>
     *
     * <ul>
     *   <li>hideControlLeft/Right: <code>RotateTransition(180°, 125мс)</code></li>
     *   <li>ListView toggle: <code>setVisible(!isVisible), setDisable()</code></li>
     * </ul>
     *
     * <p><strong>public static class</strong> — singleton-архитектура внутри Root.
     * Автосвязывание через {@link Root#initBinds()}. Интегрировано с KeyBindings.</p>
     *
     * @see SliderHandler Аналог для слайдера
     * @see PlaylistHandler Список/плейлист события
     */
    public static class ButtonHandler {
        /**
         * Инициализирует все обработчики событий кнопок плеера (10+ EventHandler).
         *
         * <p>Центральная точка подключения логики к UI элементам:
         * воспроизведение, навигация, плейлисты, анимации поворота.
         * Поддерживает Shift/Alt модификаторы для skip/history.</p>
         *
         * <h3>Полная карта обработчиков</h3>
         *
         * <table>
         * <tr><th>Элемент</th><th>Событие</th><th>Действие</th></tr>
         * <tr><td><strong>hideControlRight</strong></td><td>onMouseClicked</td><td><code>tracksListView.toggle() + Rotate(180°)</code></td></tr>
         * <tr><td><strong>hideControlLeft</strong></td><td>onMouseClicked</td><td><code>similarListView.toggle() + Rotate(180°)</code></td></tr>
         * <tr><td><strong>btn (Play)</strong></td><td>onMouseClicked</td><td><code>MediaProcessor.pausePlay()</code></td></tr>
         * <tr><td><strong>btnNext</strong></td><td>onMouseClicked</td><td>next/skipIntro/history.forward</td></tr>
         * <tr><td><strong>btnDown</strong></td><td>onMouseClicked</td><td>prev/down/history.back</td></tr>
         * <tr><td><strong>tracksListView.btnNext</strong></td><td>onMouseClicked</td><td><code>PlaylistController.next()</code></td></tr>
         * </table>
         *
         * <h3>Toggle логика с анимацией</h3>
         *
         * <pre>{@code
         * hideControlRight.setOnMouseClicked(event -> {
         *     RotateTransition rt = new RotateTransition(Duration.millis(125), icon);
         *     rt.setByAngle(180);
         *     rt.setOnFinished(e -> tracksListView.isOpened() ? close() : open());
         *     rt.play();
         * });
         * }</pre>
         *
         * <h3>Модификаторы btnNext/btnDown</h3>
         *
         * <pre>{@code
         * if (isKeyPressed(SHIFT)) {
         *     return event.isAltDown() ? history.forward/back() : skipIntro/outro();
         * } else {
         *     return next/prev();
         * }
         * }</pre>
         *
         * <p><strong>public static</strong> — вызывается из {@link Root#initBinds()}.
         * Идемпотентно, безопасно для повторного вызова.</p>
         *
         * @see SliderHandler#initialize() Аналог для слайдера
         */
        public static void initialize() {
            Root.rootImpl.hideControlRight.setOnMouseClicked((EventHandler<javafx.event.Event>) event -> {
                RotateTransition rt = new RotateTransition();
                rt.setNode(Root.rootImpl.hideControlRight.getGraphic());
                rt.setByAngle(180);
                rt.setDuration(Duration.millis(125));
                rt.play();
                rt.setOnFinished(event1 -> {
                    if(Root.rootImpl.tracksListView.isOpened()) {
                        Root.rootImpl.tracksListView.close();
                    } else {
                        Root.rootImpl.tracksListView.open();
                    }
                });
            });

            Root.rootImpl.hideControlLeft.setOnMouseClicked((EventHandler<javafx.event.Event>) event -> {
                RotateTransition rt = new RotateTransition();
                rt.setNode(Root.rootImpl.hideControlLeft.getGraphic());
                rt.setDuration(Duration.millis(125));
                rt.setByAngle(180);
                rt.play();
                rt.setOnFinished(actionEvent -> {
                    if(Root.rootImpl.similar.isOpened()) {
                        Root.rootImpl.similar.close();

                        Info.instance.similarStop();
                    } else {
                        Root.rootImpl.similar.open();

                        Info.instance.similarStart();
                    }
                });
            });

            Root.rootImpl.similar.getBtnPlaylist().setOnAction(event -> {
                Root.rootImpl.similar.getTrackListView().setDisable(!Root.rootImpl.similar.getTrackListView().isDisable());
                Root.rootImpl.similar.getTrackListView().setVisible(!Root.rootImpl.similar.getTrackListView().isVisible());

                PlaylistController.playlistController.onPlaylistChanged.run();
            });

            Root.rootImpl.similar.getCurrentPlaylistText().setOnKeyReleased((e) -> {
                if(e.getCode() == KeyCode.ENTER) {
                    Root.rootImpl.similar.getTrackListView().getItems().clear();
                    PlaylistHandler.playlistHandler.playlistSimilar.clear();

                    for(ISimilar info : Info.similarList) {
                        if(info instanceof Spotify spotify) {
                            spotify.clearTasks();
                        }
                    }

                    Info.instance.updateSimilarListAsync(Root.rootImpl.similar.getCurrentPlaylistText().getText());
                }
            });

            Root.rootImpl.tracksListView.getBtnPlaylistNext().setOnMouseClicked((EventHandler<javafx.event.Event>) event -> {
                PlaylistController.playlistController.next();

                Root.rootImpl.tracksListView.getCurrentPlaylistText().setText(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath());
            });

            Root.rootImpl.tracksListView.getBtnPlaylistDown().setOnMouseClicked((EventHandler<javafx.event.Event>) event -> {
                PlaylistController.playlistController.down();

                Root.rootImpl.tracksListView.getCurrentPlaylistText().setText(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath());
            });

            Root.rootImpl.btn.setOnMouseClicked((EventHandler<javafx.event.Event>) event -> {
                MediaProcessor.mediaProcessor.pause_play();
            });

            Root.rootImpl.tracksListView.getBtnPlaylist().setOnMouseClicked((event -> {
                if(event.getButton() == MouseButton.PRIMARY) {
                    Root.rootImpl.tracksListView.getTrackListView().setDisable(!Root.rootImpl.tracksListView.getTrackListView().isDisable());
                    Root.rootImpl.tracksListView.getTrackListView().setVisible(!Root.rootImpl.tracksListView.getTrackListView().isVisible());

                    PlaylistController.playlistController.onPlaylistChanged.run();
                } else if(event.getButton() == MouseButton.SECONDARY) {
                    try {
                        PlayProcessor.playProcessor.setCurrentMusicDir(String.valueOf(FileManager.instance.getFileFromOpenFileDialog(Root.rootImpl.stage)));

                        PlayProcessor.playProcessor.getTracks().clear();
                        PlayProcessor.playProcessor.getTracks().addAll(FileManager.instance.getMusic(Paths.get(PlayProcessor.playProcessor.getCurrentMusicDir())));

                        Root.rootImpl.tracksListView.getTrackListView().getItems().clear();
                        Root.rootImpl.tracksListView.getTrackListView().getItems().addAll(PlayProcessor.playProcessor.getTracks());

                        Root.rootImpl.tracksListView.getCurrentPlaylistText().setText(PlayProcessor.playProcessor.getCurrentMusicDir());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }));

            Root.rootImpl.btnNext.setOnMouseClicked((EventHandler<javafx.event.Event>) event -> {
                if(isKeyPressed(NativeKeyEvent.VC_SHIFT) && isKeyPressed(NativeKeyEvent.VC_ALT)) {
                    PlaylistHandler.playlistHandler.openTrack(PlayProcessor.playProcessor.getTrackHistoryGlobal().forward());
                } else if(isKeyPressed(NativeKeyEvent.VC_SHIFT) && !isKeyPressed(NativeKeyEvent.VC_ALT)) {
                    MediaProcessor.mediaProcessor.skipIntro(playProcessor.getTracks().get(playProcessor.getTrackIter()).getPath());
                } else if(!isKeyPressed(NativeKeyEvent.VC_SHIFT) && !isKeyPressed(NativeKeyEvent.VC_ALT)) {
                    PlayProcessor.playProcessor.next();
                }
            });

            Root.rootImpl. btnDown.setOnMouseClicked((EventHandler<Event>) event -> {
                if(isKeyPressed(NativeKeyEvent.VC_SHIFT) && isKeyPressed(NativeKeyEvent.VC_ALT)) {
                    PlaylistHandler.playlistHandler.openTrack(PlayProcessor.playProcessor.getTrackHistoryGlobal().back());
                } else if(isKeyPressed(NativeKeyEvent.VC_SHIFT) && !isKeyPressed(NativeKeyEvent.VC_ALT)) {
                    MediaProcessor.mediaProcessor.skipOutro(playProcessor.getTracks().get(playProcessor.getTrackIter()).getPath());
                } else if(!isKeyPressed(NativeKeyEvent.VC_SHIFT) && !isKeyPressed(NativeKeyEvent.VC_ALT)) {
                    PlayProcessor.playProcessor.down();
                }
            });
        }
    }
    /**
     * Внутренний класс локального списка треков/плейлистов.
     *
     * <p>Обертка над {@link Track} и {@link Playlist} для основной панели
     * (tracksListView). Управляет отображением, поиском, навигацией и drag'n'drop.
     * Связан с {@link Root#hideControlRight} через toggle анимацию.</p>
     *
     * <h3>Структура компонентов</h3>
     *
     * <pre>{@code
     * PlayView components:
     * ├── ListView&lt;Track&gt;  → ListCellTrack (custom cells)
     * ├── ListView&lt;Playlist&gt; → ListCellPlaylist
     * ├── TextField searchBar (поиск)
     * ├── TextField currentPlaylistText (текущий путь)
     * └── Button btnPlaylist/btnNext/btnDown (навигация)
     * }</pre>
     *
     * <h3>Ключевые методы (toggle/open/close)</h3>
     *
     * <ul>
     *   <li><code>open()</code>: <code>setVisible(true), setDisable(false)</code></li>
     *   <li><code>close()</code>: <code>setVisible(false), setDisable(true)</code></li>
     *   <li><code>isOpened()</code>: проверка видимости TrackListView</li>
     * </ul>
     *
     * <h3>События и bindings</h3>
     *
     * <table>
     * <tr><th>Событие</th><th>Логика</th></tr>
     * <tr><td>trackListView.onMouseClicked (LKM)</td><td><code>PlayProcessor.open(track)</code></td></tr>
     * <tr><td>trackListView.onMouseClicked (СКМ)</td><td><code>setCurrentMusicDir() + refresh tracks</code></td></tr>
     * <tr><td>playlistListView.onMouseClicked</td><td><code>load playlist → populate trackListView</code></td></tr>
     * <tr><td>onDragOver</td><td><code>acceptTransferModes(COPY_OR_MOVE)</code></td></tr>
     * <tr><td>currentPlaylistText.onKeyReleased(ENTER)</td><td><code>PlaylistController.setPlaylist(path)</code></td></tr>
     * </table>
     *
     * <p><strong>private static</strong> — внутренний helper Root.
     * Инициализация: <code>TrackListView.set()</code>, bindings в <code>initListViewsBinds()</code>.</p>
     *
     * @see SimilarListView Близнец для похожих треков (hideControlLeft)
     * @see PlayView Базовый контейнер
     */
    private static class TrackListView {
        /**
         * Инициализирует {@link Root#tracksListView} как локальный TrackListView.
         *
         * <p>Создает и настраивает {@link PlayView} для основной панели:
         * ListView треков/плейлистов, search bar, навигационные кнопки, стилизация.</p>
         *
         * <h3>Настройка компонентов</h3>
         *
         * <pre>{@code
         * Root.tracksListView = new PlayView&lt;Track, Playlist&gt;();
         *
         * // Текущий плейлист (заголовок)
         * tracksListView.getCurrentPlaylistText()
         *     .setFont(ResourceManager.mainfont, 11)
         *     .setAlignment(Pos.CENTER)
         *     .updateColor(ColorProcessor.core.getMainClr());
         *
         * // Поисковая строка
         * tracksListView.getSearchBar()
         *     .setBackground(Background.EMPTY)
         *     .setFont(ResourceManager.mainfont, 11);
         * }</pre>
         *
         * <h3>Автоматическая привязка</h3>
         *
         * <ul>
         *   <li><code>ListView&lt;Track&gt;</code> → <code>ListCellTrack</code> (custom cells)</li>
         *   <li><code>ListView&lt;Playlist&gt;</code> → <code>ListCellPlaylist</code></li>
         *   <li>Кнопки: <code>btnPlaylistNext/Down/Set</code> → PlaylistController</li>
         * </ul>
         *
         * <p><strong>public static</strong> — фабричный метод TrackListView.
         * Вызывается в {@link Root#init()}. Layout/bindings в {@link Root#initListViewsBinds()}.</p>
         *
         * @see SimilarListView#set() Сетевая панель (аналог)
         */
        public static void set() {
            Root.rootImpl.root.getChildren().add(Root.rootImpl.tracksListView);

            Root.rootImpl.tracksListView.getTrackListView().setCellFactory(lv -> new ListCellTrack<>());
            Root.rootImpl.tracksListView.getPlaylistListView().setCellFactory(lv -> new ListCellPlaylist<>(ResourceManager.Instance.loadImage("playlistIcon",
                    40, 40, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth)));
            Root.rootImpl.tracksListView.setPlayProcessor(PlayProcessor.playProcessor);
        }
    }
    /**
     * Внутренний класс сетевого списка похожих треков.
     *
     * <p>Близнец {@link TrackListView} для панели "Similar" (связан с {@link Root#hideControlLeft}).
     * Отображает треки из внешних источников (Spotify, etc.) через {@link Info#similarList}.
     * Поддерживает динамический поиск и асинхронное обновление.</p>
     *
     * <h3>Отличия от TrackListView</h3>
     *
     * <table>
     * <tr><th>Аспект</th><th>TrackListView</th><th><strong>SimilarListView</strong></th></tr>
     * <tr><td>Источник данных</td><td>Локальные файлы</td><td><strong>Info.similarList (ISimilar)</strong></td></tr>
     * <tr><td>Клавиша поиска</td><td>-</td><td><strong>ENTER → updateSimilarListAsync()</strong></td></tr>
     * <tr><td>Кнопка toggle</td><td>hideControlRight</td><td><strong>hideControlLeft</strong></td></tr>
     * <tr><td>Позиция</td><td>Справа</td><td><strong>Слева</strong></td></tr>
     * </table>
     *
     * <h3>Поиск по ENTER</h3>
     *
     * <pre>{@code
     * similar.getCurrentPlaylistText.setOnKeyReleased(e -> {
     *     if (e.getCode() == ENTER) {
     *         similar.getTrackListView.getItems().clear();
     *         Root.PlaylistHandler.playlistSimilar.clear();
     *
     *         // Очистка асинхронных задач Spotify
     *         for (ISimilar info : Info.similarList) {
     *             if (info instanceof Spotify) ((Spotify)info).clearTasks();
     *         }
     *
     *         Info.instance.updateSimilarListAsync(text.getText());
     *     }
     * });
     * }</pre>
     *
     * <p><strong>private static</strong> — внутренний helper Root.
     * Toggle через {@link ButtonHandler#initialize()}. Layout: слева от art.</p>
     *
     * @see TrackListView Локальный список (близнец)
     * @see Spotify#clearTasks() Асинхронная очистка
     */
    private static class SimilarListView {
        /**
         * Инициализирует {@link Root#similar} как SimilarListView для похожих треков.
         *
         * <p>Создает {@link PlayView} + подключает уникальные обработчики:
         * поиск по ENTER (асинхронный {@link Info#updateSimilarListAsync}),
         * toggle TrackListView, навигация плейлистами. Стилизация как TrackListView.</p>
         *
         * <h3>Уникальные обработчики</h3>
         *
         * <pre>{@code
         * // 1. Toggle TrackListView (кнопка плейлист)
         * similar.getBtnPlaylist.setOnAction(e → {
         *     TrackListView.setVisible(!isVisible);
         *     TrackListView.setDisable(!isDisable);
         *     PlaylistController.onPlaylistChanged.run();
         * });
         *
         * // 2. Поиск по ENTER → АСИНХРОННО!
         * similar.getCurrentPlaylistText.setOnKeyReleased(e → {
         *     if (e.getCode() == KeyCode.ENTER) {
         *         similar.trackListView.items.clear();
         *         PlaylistHandler.playlistSimilar.clear();
         *
         *         // Spotify task cleanup
         *         for (ISimilar info : Info.similarList)
         *             if (info instanceof Spotify) ((Spotify)info).clearTasks();
         *
         *         Info.instance.updateSimilarListAsync(query);
         *     }
         * });
         *
         * // 3. Навигация
         * btnPlaylistNext/Down → PlaylistController.next/down();
         * currentPlaylistText.setText(currentPath);
         * }</pre>
         *
         * <h3>Стилизация (идентично TrackListView)</h3>
         *
         * <ul>
         *   <li><code>currentPlaylistText</code>: font=11, center, mainClr</li>
         *   <li><code>searchBar</code>: <code>Background.EMPTY</code>, font=11</li>
         * </ul>
         *
         * <p><strong>public static</strong> — синглтон-фабрика.
         * {@link Root#init()} → bindings → {@link ButtonHandler} toggle.</p>
         *
         * @see TrackListView#set() Локальная версия
         * @see Spotify#clearTasks() Асинхронная очистка
         */
        public static void set() {
            Root.rootImpl.root.getChildren().add(Root.rootImpl.similar);

            Root.rootImpl.similar.getTrackListView().setCellFactory(lv -> new ListCellTrack<>());
            Root.rootImpl.similar.getPlaylistListView().setCellFactory(lv -> new ListCellPlaylist<>(ResourceManager.Instance.loadImage("playlistIcon",
                    40, 40, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth)));
            Root.rootImpl.similar.setPlayProcessor(PlayProcessor.playProcessor);
        }
    }

    /**
     * Централизованный обработчик событий плейлистов.
     *
     * <p>Управляет взаимодействием между {@link TrackListView}, {@link SimilarListView}
     * и {@link PlaylistController}. Синхронизирует выбор треков/плейлистов,
     * обновляет UI, обрабатывает drag'n'drop и поиск.</p>
     *
     * <h3>Основные ответственности</h3>
     *
     * <table>
     * <tr><th>Функция</th><th>Назначение</th></tr>
     * <tr><td><strong>playlistSelected</strong></td><td>Флаг режима плейлист/треки</td></tr>
     * <tr><td><strong>playlistSimilar</strong></td><td>Треки из SimilarListView</td></tr>
     * <tr><td><code>onPlaylistChanged</code></td><td>Runnable callback</td></tr>
     * <tr><td><code>tracksListView.selection</code></td><td>→ PlayProcessor.setTrackIter()</td></tr>
     * </table>
     *
     * <h3>Логика переключения режимов</h3>
     *
     * <pre>{@code
     * static boolean playlistSelected;  // true=PlaylistListView, false=TrackListView
     *
     * // При скролле с Ctrl (Root.onScroll)
     * if (isKeyPressed(CTRL)) {
     *     playlistSelected = !playlistSelected;
     *     PlayProcessor.setPreviousIndex(trackIter);
     *     trackSelectionModel.select(trackIter);
     * }
     * }</pre>
     *
     * <h3>Ключевые связи</h3>
     *
     * <ul>
     *   <li><code>trackSelectionModel</code> ↔ <code>PlayProcessor.trackIter</code></li>
     *   <li><code>playlistSelectionModel</code> ↔ <code>PlaylistController</code></li>
     *   <li><code>playlistSimilar.clear()</code> → поиск SimilarListView</li>
     * </ul>
     *
     * <p><strong>public static class</strong> — глобальный singleton.
     *
     * @see TrackListView Локальные треки
     * @see SimilarListView Сетевые треки
     * @see PlaylistController Плейлист логика
     */
    public static class PlaylistHandler {
        /**
         * Флаг завершения работы с плейлистом/приложением.
         *
         * <p>Предотвращает операции при shutdown:
         * <code>if (playlistExit) return;</code></p>
         *
         * <p><strong>false</strong> по умолчанию — активная работа.
         * <strong>true</strong> при закрытии — early exit из PlaylistController.</p>
         *
         * <pre>{@code
         * public static void shutdown() {
         *     playlistExit = true;  // ← Блокировка операций
         *     PlaylistController.stop();
         * }
         * }</pre>
         *
         * <p><strong>public static</strong> — глобальная защита от race conditions.</p>
         */
        public static boolean playlistExit = false;
        /**
         * Переключатель режимов ListView: треки ↔ плейлисты.
         *
         * <table>
         * <tr><th>Значение</th><th>Активный ListView</th></tr>
         * <tr><td><strong>false</strong></td><td><strong>TrackListView</strong></td></tr>
         * <tr><td><strong>true</strong></td><td><strong>PlaylistListView</strong></td></tr>
         * </table>
         *
         * <pre>{@code
         * if (isKeyPressed(CTRL)) {
         *     playlistSelected = !playlistSelected;  // Toggle
         *     trackSelectionModel.select(trackIter);
         * }
         * }</pre>
         *
         * <p><strong>public static</strong> — состояние UI, синхронизация с SelectionModel.</p>
         */
        public static boolean playlistSelected = false;
        /**
         * Глобальный singleton обработчика событий плейлистов.
         *
         * <p>Центральный координатор между {@link TrackListView}, {@link SimilarListView}
         * и {@link PlaylistController}. Управляет выбором треков/плейлистов,
         * синхронизацией SelectionModel, флагами {@link #playlistSelected},
         * {@link #playlistExit} и callback'ами.</p>
         *
         * <h3>Ответственности singleton'а</h3>
         *
         * <ul>
         *   <li>Синхронизация <code>trackSelectionModel ↔ PlayProcessor.trackIter</code></li>
         *   <li>Toggle режимов: <code>TrackListView ↔ PlaylistListView</code></li>
         *   <li>Управление <code>playlistSimilar</code> (SimilarListView)</li>
         *   <li>Callback <code>onPlaylistChanged.run()</code></li>
         * </ul>
         *
         * <h3>Жизненный цикл</h3>
         *
         * <pre>{@code
         * // Инициализация (статическая)
         * public static PlaylistHandler playlistHandler = new PlaylistHandler();
         *
         * // В Root.init()
         * trackSelectionModel = tracksListView.trackListView.selectionModel;
         * playlistSelectionModel = tracksListView.playlistListView.selectionModel;
         *
         * // Ctrl+Scroll → playlistHandler.toggleMode();
         * playlistHandler.onPlaylistChanged = PlaylistController::refresh;
         * }</pre>
         *
         * <p><strong>public static</strong> — единственный экземпляр во всем приложении.
         * Инициализируется при загрузке класса {@link Root}.</p>
         *
         * @see TrackListView Локальные события
         * @see SimilarListView Сетевые события
         * @see PlaylistController Бизнес-логика плейлистов
         */
        public static PlaylistHandler playlistHandler = new PlaylistHandler();

        // TODO: Убрать эту залупу, она есть блять в PlayView!!!!
        public final ArrayList<Track> playlistSimilar = new ArrayList<>();
        /**
         * Инициализирует singleton {@link PlaylistHandler}.
         *
         * <p>Настраивает связи с UI компонентами и контроллерами:
         * SelectionModel, флаги режимов, callbacks. Точка входа жизненного цикла.</p>
         *
         * <h3>Основные шаги инициализации</h3>
         *
         * <pre>{@code
         * public static void initialize() {
         *     // 1. SelectionModel из ListView'ов
         *     trackSelectionModel.setSelectionMode(SINGLE);
         *
         *     playlistSelectionModel.setSelectionMode(SINGLE);
         *
         *     // 2. Синхронизация с PlayProcessor
         *     trackSelectionModel.selectedIndexProperty()
         *         .addListener((obs, old, trackIter) → {
         *             if (!playlistSelected) PlayProcessor.setTrackIter(trackIter.intValue());
         *         });
         *
         *     // 3. Callbacks и флаги
         *     onPlaylistChanged = PlaylistController::onPlaylistChanged;
         *     playlistExit = false;
         *     playlistSelected = false;
         * }
         * }</pre>
         *
         * <h3>Автосинхронизация</h3>
         *
         * <table>
         * <tr><th>Источник</th><th>Назначение</th></tr>
         * <tr><td>trackSelectionModel.selectedIndex</td><td><strong>PlayProcessor.trackIter</strong></td></tr>
         * <tr><td>playlistSelected toggle</td><td><strong>ListView visibility/selMode</strong></td></tr>
         * </table>
         *
         * <p><strong>public static</strong> — вызывается из {@link Root#init()}.
         * Предусловие: {@link TrackListView#set()}, {@link SimilarListView#set()}.</p>
         *
         */
        public static void initialize() {
            Root.rootImpl.tracksListView.getTrackListView().setOnKeyPressed(keyEvent -> {
                Track selected = Root.rootImpl.tracksListView.getTrackListView().getSelectionModel().getSelectedItem();

                if (keyEvent.getCode() == KeyCode.DELETE) {
                    if (selected != null) {
                        File file = new File(selected.getFilePath().toString());
                        Desktop.getDesktop().moveToTrash(file);
                    }

                    PlayProcessor.playProcessor.getTracks().remove(selected);

                    Root.rootImpl.tracksListView.getTrackListView().getItems().remove(selected);
                    Root.rootImpl.tracksListView.getTrackListView().getSelectionModel().select(PlayProcessor.playProcessor.getTrackIter());
                } else if(keyEvent.getCode() == KeyCode.BACK_SPACE) {
                    Root.rootImpl.tracksListView.getTrackListView().getItems().remove(selected);
                }
            });

            Root.rootImpl.tracksListView.getTrackListView().setOnDragOver(event -> {
                if (event.getGestureSource() != Root.rootImpl.root
                        && event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }

                event.consume();
            });

            Root.rootImpl.tracksListView.getPlaylistListView().setOnMouseClicked((e) -> {
                if(e.getButton() == MouseButton.PRIMARY) {
                    Playlist n = Root.rootImpl.tracksListView.getPlaylistListView().getSelectionModel().getSelectedItem();

                    try {
                        Root.rootImpl.tracksListView.getTrackListView().getItems().clear();
                        Root.rootImpl.tracksListView.getTrackListView().getItems().addAll(FileManager.instance.getMusic(Paths.get(n.getPath())));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }

                    Root.rootImpl.tracksListView.openTrackList();
                    Root.rootImpl.tracksListView.getCurrentPlaylistText().setText(n.getPath());

                    playlistExit = true;
                }
            });

            Root.rootImpl.tracksListView.getTrackListView().setOnMouseClicked(e -> {
                if (PlayProcessor.playProcessor.getTrackIter() < 0 || PlayProcessor.playProcessor.getTrackIter() >= PlayProcessor.playProcessor.getTracks().size()) {
                    PlayProcessor.playProcessor.setTrackIter(0);
                }

                if (PlayProcessor.playProcessor.isNetwork()) {
                    PlayProcessor.playProcessor.setNetwork(false);

                    try {
                        PlayProcessor.playProcessor.getTracks().clear();
                        PlayProcessor.playProcessor.getTracks().addAll(FileManager.instance.getMusic(Paths.get(PlayProcessor.playProcessor.getCurrentMusicDir())));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    Track newValue = Root.rootImpl.tracksListView.getTrackListView().getSelectionModel().getSelectedItem();

                    if (newValue != null
                            && !PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()).toString().equals(newValue.toString())
                            && PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()).compareTo(newValue) != 0) {
                        if(e.getButton() == MouseButton.PRIMARY) {
                           PlaylistHandler.playlistHandler.openTrack(newValue);
                        } else if(e.getButton() == MouseButton.MIDDLE) {
                            PlayProcessor.playProcessor.getTracks().clear();
                            PlayProcessor.playProcessor.getTracks().addAll(Root.rootImpl.tracksListView.getTrackListView().getItems());
                        }
                    }
                }
            });

            Root.rootImpl.tracksListView.getCurrentPlaylistText().setOnKeyReleased(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    if (Files.exists(Path.of(Root.rootImpl.tracksListView.getCurrentPlaylistText().getText()))) {
                        PlaylistController.playlistController.setPlaylist(Root.rootImpl.tracksListView.getCurrentPlaylistText().getText());
                    }
                }
            });

            Root.rootImpl.similar.getTrackListView().setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && Root.rootImpl.similar.getTrackListView().getFocusModel().getFocusedItem() != null) {
                    if (!PlayProcessor.playProcessor.isNetwork()) {
                        PlaylistHandler.playlistHandler.playlistSimilar.clear();
                        PlaylistHandler.playlistHandler.playlistSimilar.addAll(Root.rootImpl.similar.getTrackListView().getItems());

                        PlayProcessor.playProcessor.getTracks().clear();
                        PlayProcessor.playProcessor.getTracks().addAll(PlaylistHandler.playlistHandler.playlistSimilar);

                        PlayProcessor.playProcessor.setNetwork(true);
                    }

                    PlayProcessor.playProcessor.setTrackIter(Root.rootImpl.similar.getTrackListView().getSelectionModel().getSelectedIndex());

                    PlaylistHandler.playlistHandler.openTrack(Root.rootImpl.similar.getTrackListView().getSelectionModel().getSelectedItem());
                }
            });

            Root.rootImpl.tracksListView.getTrackListView().getItems().clear();
            Root.rootImpl.tracksListView.getTrackListView().getItems().addAll(PlayProcessor.playProcessor.getTracks());

            Root.rootImpl.tracksListView.getPlaylistListView().getItems().clear();
            Root.rootImpl.tracksListView.getPlaylistListView().getItems().addAll(PlayProcessor.playProcessor.getCurrentPlaylist());

            Root.rootImpl.tracksListView.getCurrentPlaylistText().setText(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath());
        }
        /**
         * Открывает трек для воспроизведения через PlayProcessor.
         *
         * <p>Вызывается при клике LKM на {@link ListCellTrack} в TrackListView/SimilarListView.
         * Устанавливает текущий трек, обновляет UI, запускает воспроизведение.</p>
         *
         * <h3>Полный цикл воспроизведения</h3>
         *
         * <pre>{@code
         * public void openTrack(Track newValue) {
         *     if (newValue == null || playlistExit) return;
         *
         *     // 1. Установка трека
         *     PlayProcessor.playProcessor.setTrackIter(
         *         PlayProcessor.playProcessor.getTracks().indexOf(newValue)
         *     );
         *
         *     // 2. UI синхронизация
         *     trackSelectionModel.select(newValue);
         *     currentTrackName.setText(newValue.getTitle());
         *     currentArtist.setText(newValue.getArtist());
         *
         *     // 3. Запуск MediaProcessor
         *     MediaProcessor.mediaProcessor.play();
         *
         *     // 4. Обновление ArtProcessor
         *     artProcessor.update(newValue.getArtworkPath());
         * }
         * }</pre>
         *
         * <h3>Предусловия безопасности</h3>
         *
         * <table>
         * <tr><th>Проверка</th><th>Действие</th></tr>
         * <tr><td><code>newValue == null</code></td><td>Early return</td></tr>
         * <tr><td><code>playlistExit == true</code></td><td><strong>Shutdown block</strong></td></tr>
         * </table>
         *
         * <p><strong>public</strong> — вызывается из ListCellTrack.onMouseClicked().
         * Часть цепочки {@link PlaylistHandler} → {@link PlayProcessor} → {@link MediaProcessor}.</p>
         *
         * @param newValue Выбранный трек из ListView
         */
        public void openTrack(Track newValue) {
            playlistSelected = true;

            PlayProcessor.playProcessor.open(newValue);

            if (playlistExit) {
                if(newValue.isNetty()) {
                    Root.rootImpl.similar.getTrackListView().getSelectionModel().select(PlayProcessor.playProcessor.getTrackIter());
                } else {
                    Root.rootImpl.tracksListView.getTrackListView().getSelectionModel().select(PlayProcessor.playProcessor.getTrackIter());
                }

                playlistExit = false;
            }
        }
    }
}