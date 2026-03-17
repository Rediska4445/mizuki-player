package rf.ebanina.UI.UI.Element;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.CacheHint;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;

/**
 * <h1>Art</h1>
 * Графический компонент для отображения обложки/арт-изображения с закруглёнными углами.
 * <p>
 * Класс наследует {@link Rectangle} и заполняет его содержимым {@link Image} через {@link ImagePattern},
 * что делает его удобным для использования как универсальный виджет "обложки": постер трека, альбом, фон и т.п.
 * Внутри поддерживается текущее и предыдущее изображение, что упрощает реализацию анимаций смены арта,
 * откатов и плавных переходов.
 * </p>
 *
 * <h3>Особенности</h3>
 * <ul>
 *   <li>Закруглённые углы за счёт настроек {@link Rectangle#setArcWidth(double)} и {@link Rectangle#setArcHeight(double)}.</li>
 *   <li>Отрисовка изображения как паттерна с помощью {@link ImagePattern}, а не через {@code ImageView}.</li>
 *   <li>Хранение ссылки на предыдущее изображение для удобства анимаций и откатов состояния.</li>
 *   <li>Использование {@link SimpleObjectProperty} для интеграции со связями JavaFX и реактивными сценариями.</li>
 *   <li>Установка {@link CacheHint#QUALITY} для улучшения качества масштабирования и рендеринга.</li>
 * </ul>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * Art art = new Art(16);
 * art.setWidth(128);
 * art.setHeight(128);
 *
 * Image cover = new Image("file:cover.png");
 * art.setImage(cover);
 *
 * // Добавление в сцену
 * root.getChildren().add(art);
 *
 * // Доступ к предыдущему изображению (например, для анимации перекрёстного затухания)
 * Image oldCover = art.getPreviousImage();
 * }</pre>
 *
 * <h3>Назначение полей</h3>
 * <ul>
 *   <li><b>image</b> — текущее изображение, используемое для заливки прямоугольника.</li>
 *   <li><b>previousImage</b> — предыдущее изображение, автоматически обновляется при каждом вызове {@link #setImage(Image)}.</li>
 * </ul>
 *
 * Класс не управляет загрузкой ресурсов и анимациями самостоятельно, а лишь предоставляет удобный контейнер
 * с сохранением истории изображений и визуальным оформлением в виде закруглённого прямоугольника.
 *
 * @author Ebanina Std
 * @since 0.1.4.7
 * @see Rectangle
 * @see Image
 * @see ImagePattern
 */
public class Art
        extends Rectangle
{
    /**
     * Текущее изображение, используемое для заливки прямоугольника.
     * <p>
     * Значение устанавливается методом {@link #setImage(Image)} и может
     * быть прочитано через {@link #getImage()} или свойство {@link #imageProperty()}.
     * </p>
     */
    private SimpleObjectProperty<Image> image = new SimpleObjectProperty<>();
    /**
     * Предыдущее изображение, использовавшееся до последнего вызова {@link #setImage(Image)}.
     * <p>
     * Поле автоматически обновляется при установке нового изображения и может быть использовано
     * для анимаций перехода между артом или для отката к старому состоянию.
     * </p>
     */
    private SimpleObjectProperty<Image> previousImage = new SimpleObjectProperty<>();
    /**
     * Создаёт новый элемент {@code Art} с указанным радиусом закругления углов.
     * <p>
     * В конструкторе настраивается:
     * <ul>
     *   <li>Кэширование узла с подсказкой {@link CacheHint#QUALITY} для более качественного рендеринга.</li>
     *   <li>Параметры {@link Rectangle#arcWidthProperty()} и {@link Rectangle#arcHeightProperty()} для закругления углов.</li>
     * </ul>
     * </p>
     *
     * @param corners радиус закругления углов прямоугольника в пикселях
     */
    public Art(int corners) {
        setCacheHint(CacheHint.QUALITY);
        setArcHeight(corners);
        setArcWidth(corners);
    }
    /**
     * Устанавливает новое изображение и обновляет заливку прямоугольника.
     * <p>
     * Перед сменой изображения текущее значение из {@link #image} сохраняется в {@link #previousImage},
     * после чего в качестве заливки вызывается {@link #setFill(javafx.scene.paint.Paint)} с {@link ImagePattern}.
     * Если внутреннее поле {@link #image} по какой-либо причине равно {@code null}, метод ничего не делает.
     * </p>
     *
     * @param image новое изображение для отображения; может быть {@code null}, тогда заливка устанавливается с этим значением
     */
    public void setImage(Image image) {
        if(this.image == null) {
            return;
        }

        previousImage.set(this.image.get());

        this.image.set(image);
        setFill(new ImagePattern(image));
    }
    /**
     * Возвращает предыдущее изображение, если оно доступно.
     * <p>
     * Если {@link #previousImage} ещё не было установлено (например, это первое изображение),
     * метод возвращает текущее {@link #getImage()}, чтобы гарантировать не {@code null}-результат
     * при типичных сценариях использования.
     * </p>
     *
     * @return предыдущее изображение или текущее, если предыдущего нет
     */
    public Image getPreviousImage() {
        return previousImage.get() == null ? image.get() : previousImage.get();
    }
    /**
     * Возвращает текущее изображение, привязанное к этому элементу.
     *
     * @return текущее изображение или {@code null}, если оно ещё не установлено
     */
    public Image getImage() {
        return image.get();
    }
    /**
     * Свойство текущего изображения.
     * <p>
     * Может использоваться для биндинга, наблюдения за изменениями
     * и интеграции с другими компонентами JavaFX.
     * </p>
     *
     * @return свойство с текущим изображением
     */
    public SimpleObjectProperty<Image> imageProperty() {
        return image;
    }
    /**
     * Свойство предыдущего изображения.
     * <p>
     * Полезно, если требуется реагировать на смену "истории" арта
     * или синхронизировать анимации с конкретными значениями.
     * </p>
     *
     * @return свойство с предыдущим изображением
     */
    public SimpleObjectProperty<Image> previousImageProperty() {
        return previousImage;
    }
    /**
     * Принудительно задаёт предыдущее изображение.
     * <p>
     * Обычно это поле обновляется автоматически в {@link #setImage(Image)}, но данный метод
     * позволяет вручную переопределить его, если логика приложения этого требует.
     * </p>
     *
     * @param previousImage изображение, которое следует считать предыдущим
     */
    public void setPreviousImage(Image previousImage) {
        this.previousImage.set(previousImage);
    }
}