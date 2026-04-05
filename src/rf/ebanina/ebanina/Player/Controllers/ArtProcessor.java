package rf.ebanina.ebanina.Player.Controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import org.json.simple.parser.ParseException;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.Network.IParseAlbumArt;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Animations;
import rf.ebanina.UI.UI.Element.Buttons.Button;
import rf.ebanina.UI.UI.Element.ListViews.Playlist.PlayView;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static rf.ebanina.UI.Root.PlaylistHandler.playlistSelected;
import static rf.ebanina.UI.Root.*;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.*;

/**
 * <h1>ArtProcessor</h1>
 * Реализация IArtProcessor для динамической синхронизации цветовой схемы UI с обложками треков.
 * <p>
 * Управляет полной анимацией смены обложек: slide/fade/scale эффекты + плавное изменение цветов.
 * Использует ColorProcessor для извлечения доминирующих цветов и Animations для thread-safe анимаций.
 * </p>
 *
 * <h3>Основной цикл работы:</h3>
 * <pre>
 * Track → getAlbumArt() → ColorProcessor → extract colors →
 * update UI colors → slide/fade/scale animation → setIcon()
 * </pre>
 *
 * <h3>Анимационные параметры:</h3>
 * <table border="1">
 *   <tr><th>Параметр</th><th>Значение</th><th>Назначение</th></tr>
 *   <tr><td>durationIn/Out</td><td>250ms</td><td>Скорость slide анимации</td></tr>
 *   <tr><td>scaleOutTarget</td><td>0.7</td><td>Масштаб при выезде</td></tr>
 *   <tr><td>acceleratingInterpolator</td><td>SPLINE(0,0,0.4,1)</td><td>Material Design easing</td></tr>
 * </table>
 *
 * <h3>Threading модель:</h3>
 * <ul>
 *   <li>Color analysis: ExecutorService (single thread)</li>
 *   <li>UI updates: Platform.runLater()</li>
 *   <li>Animations: Animations.play() (focus-aware)</li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 1.4.7
 * @implements IArtProcessor
 * @see ColorProcessor
 * @see Animations
 * @see Track
 */
public class ArtProcessor
        implements IArtProcessor
{
    /**
     * Длительность анимации выезда обложки (slide/fade/scale).
     * <p>
     * Стандартное значение Material Design: 250ms.
     * </p>
     */
    public Duration durationIn = Duration.millis(250);
    /**
     * Длительность анимации заезда обложки (slide/fade/scale).
     * <p>
     * Стандартное значение Material Design: 250ms.
     * </p>
     */
    public Duration durationOut = Duration.millis(250);
    /**
     * Длительность плавного изменения цветов UI элементов.
     * <p>
     * Используется в {@link #animateColorChange}.
     * </p>
     */
    public Duration animateColorChangeDura = Duration.millis(250);
    /**
     * Целевой масштаб обложки при выезде (slide out).
     * <p>
     * 70% от исходного размера создает эффект "сжатия" при уходе.
     * </p>
     */
    public double scaleOutTarget = 0.7;
    /**
     * Начальный масштаб обложки при заезде (slide in).
     * <p>
     * Синхронизирован с {@link #scaleOutTarget} для плавного перехода.
     * </p>
     */
    public double scaleInStart = 0.7;
    /**
     * Конечный масштаб обложки (нормальный размер).
     */
    public double scaleInEnd = 1.0;
    /**
     * Material Design ускоряющий интерполятор для slide анимаций.
     * <p>
     * SPLINE(0.0, 0.0, 0.4, 1.0) — стандартная кривая Material Design:
     * </p>
     * <ul>
     *   <li>Начало: медленное (ease-in)</li>
     *   <li>Конец: быстрое (ease-out)</li>
     * </ul>
     * <p>Используется во всех slide/fade/scale переходах обложек.</p>
     */
    public Interpolator acceleratingInterpolator = Interpolator.SPLINE(0.0, 0.0, 0.4, 1.0);
    /**
     * Комплексная анимация выезда обложки.
     * <p>
     * Содержит: TranslateTransition + FadeTransition + ScaleTransition.
     * </p>
     */
    protected ParallelTransition outTransition;
    /**
     * Комплексная анимация заезда обложки.
     * <p>
     * Содержит: TranslateTransition + FadeTransition + ScaleTransition.
     * </p>
     */
    protected ParallelTransition inTransition;
    /**
     * Анимация тени при заезде обложки (inDropShadow).
     */
    protected Animation inDropShadowAnimation;
    /**
     * Анимация тени при выезде обложки (outDropShadow).
     */
    protected Animation outDropShadowAnimation;
    /**
     * Мьютекс для thread-safe работы с изображениями обложек.
     * <p>
     * Синхронизирует доступ между ExecutorService (color analysis) и FX Platform thread.
     * </p>
     * <p>Используется в {@link #initColor} и {@link #initArt}.</p>
     */
    protected final Object imageLock = new Object();
    /**
     * {@inheritDoc}
     * <p>
     * Координирует полное обновление цветовой схемы UI:
     * тексты → чекбоксы → кнопки.
     * </p>
     */
    @Override
    public void updateColors(Color color) {
        updateTextsColors(color);
        updateCheckBoxColors(color);
        updateButtonsColors(color);
    }
    /**
     * Обновляет цвета hover/pressed состояний всех кнопок приложения.
     * <p>
     * <b>Группы кнопок:</b>
     * </p>
     * <ul>
     *   <li>Основные транспортные кнопки (play/pause, next, down)</li>
     *   <li>Кнопки плейлистов в TrackListView и Similar</li>
     *   <li>MainButton (если Button)</li>
     *   <li>Кнопки скрытия панелей (hideControlLeft/Right)</li>
     * </ul>
     * <p>
     * Устанавливает <code>iconHover</code> и <code>bgPressed</code> цвета.
     * </p>
     */
    public void updateButtonsColors(Color color) {
        btn.setColorIconHover(color);
        btnNext.setColorIconHover(color);
        btnDown.setColorIconHover(color);

        btn.setColorBgPressed(color);
        btnNext.setColorBgPressed(color);
        btnDown.setColorBgPressed(color);

        tracksListView.getBtnPlaylist().setColorIconHover(color);
        ((Button) tracksListView.getBtnPlaylistNext()).setColorIconHover(color);
        ((Button) tracksListView.getBtnPlaylistDown()).setColorIconHover(color);

        tracksListView.getBtnPlaylist().setColorBgPressed(color);
        ((Button) tracksListView.getBtnPlaylistNext()).setColorBgPressed(color);
        ((Button) tracksListView.getBtnPlaylistDown()).setColorBgPressed(color);

        similar.getBtnPlaylist().setColorIconHover(color);
        ((Button) similar.getBtnPlaylistNext()).setColorIconHover(color);
        ((Button) similar.getBtnPlaylistDown()).setColorIconHover(color);

        similar.getBtnPlaylist().setColorBgPressed(color);
        ((Button) similar.getBtnPlaylistNext()).setColorBgPressed(color);
        ((Button) similar.getBtnPlaylistDown()).setColorBgPressed(color);

        if(Root.mainFunctions.getMainButton() instanceof Button i) {
            i.setColorIconHover(color);
            i.setColorBgPressed(color);
        }

        hideControlLeft.setColorIconHover(color);
        hideControlRight.setColorIconHover(color);

        hideControlLeft.setColorBgPressed(color);
        hideControlRight.setColorBgPressed(color);
    }
    /**
     * Обновляет цвет текста чекбокса плейлиста с учетом фокуса окна.
     * <p>
     * <b>Логика:</b>
     * </p>
     * <ul>
     *   <li>Без фокуса: мгновенная замена цвета</li>
     *   <li>С фокусом: плавная анимация {@link #animateColorChange}</li>
     * </ul>
     */
    public void updateCheckBoxColors(Color color) {
        if(!stage.isFocused()) {
            tracksListView.getBtnPlaylist().setTextFill(color);
        } else {
            animateColorChange(tracksListView.getBtnPlaylist().getTextFill(), color, tracksListView.getBtnPlaylist().textFillProperty());
        }
    }
    /**
     * Обновляет цвета текстовых элементов UI.
     * <p>
     * <b>FIXME:</b> Требует рефакторинга в класс Animations для унификации.
     * </p>
     * <p><b>Элементы:</b></p>
     * <ul>
     *   <li>currentArtist, currentTrackName (текущий трек)</li>
     *   <li>beginTime, endTime (таймеры)</li>
     *   <li>soundSlider</li>
     *   <li>SearchBar и PlaylistText в TrackListView/Similar</li>
     * </ul>
     * <p><b>Логика:</b> мгновенно (без фокуса) / анимировано (с фокусом)</p>
     */
    protected void updateTextsColors(Color color) {
        if(!stage.isFocused()) {
            currentArtist.updateColor(color);
            currentTrackName.updateColor(color);
            beginTime.updateColor(color);
            endTime.updateColor(color);
            soundSlider.setColor(color);

            tracksListView.getCurrentPlaylistText().updateColor(color);

            similar.getCurrentPlaylistText().updateColor(color);
        } else {
            animateColorChange(soundSlider.getColorProperty().get(), color, soundSlider.getColorProperty());
            animateColorChange(currentArtist.getColorProperty().get(), color, currentArtist.getColorProperty());
            animateColorChange(currentTrackName.getColorProperty().get(), color, currentTrackName.getColorProperty());
            animateColorChange(beginTime.getColorProperty().get(), color, beginTime.getColorProperty());
            animateColorChange(endTime.getColorProperty().get(), color, endTime.getColorProperty());

            animateColorChange(tracksListView.getSearchBar().getColorProperty().get(), color, tracksListView.getSearchBar().getColorProperty());
            animateColorChange(tracksListView.getCurrentPlaylistText().getColorProperty().get(), color, tracksListView.getCurrentPlaylistText().getColorProperty());

            animateColorChange(similar.getSearchBar().getColorProperty().get(), color, similar.getSearchBar().getColorProperty());
            animateColorChange(similar.getCurrentPlaylistText().getColorProperty().get(), color, similar.getCurrentPlaylistText().getColorProperty());
        }
    }
    /**
     * Создает плавную анимацию изменения цвета (250ms).
     * <p>
     * Thread-safe: запускается через {@link Platform#runLater}.
     * Гарантирует финальное значение цвета через onFinished.
     * </p>
     *
     * @param startColor начальный цвет
     * @param endColor целевой цвет
     * @param colorProperty свойство для анимации
     * @return Timeline объект анимации
     */
    protected Timeline animateColorChange(Color startColor, Color endColor, ObjectProperty<Color> colorProperty) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(colorProperty, startColor)),
                new KeyFrame(animateColorChangeDura, new KeyValue(colorProperty, endColor))
        );

        timeline.setOnFinished((e) -> colorProperty.set(endColor));

        Platform.runLater(timeline::play);

        return timeline;
    }
    /**
     * Создает плавную анимацию изменения Paint (Color/LinearGradient).
     * <p>
     * Не thread-safe: вызывается только из FX Platform thread.
     * Используется для stroke/fill анимаций.
     * </p>
     *
     * @param startColor начальный Paint
     * @param endColor целевой Paint
     * @param colorProperty свойство для анимации
     */
    protected void animateColorChange(Paint startColor, Paint endColor, ObjectProperty<Paint> colorProperty) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(colorProperty, startColor)),
                new KeyFrame(animateColorChangeDura, new KeyValue(colorProperty, endColor))
        );

        timeline.play();
    }
    /**
     * Устанавливает цвет фона root Pane с crossfade переходом.
     * <p>
     * <b>Логика:</b>
     * </p>
     * <ul>
     *   <li>Устанавливает BackgroundFill для Pane</li>
     *   <li>Crossfade между background/background_under ImageView</li>
     * </ul>
     * <p>Используется при смене обложки альбома.</p>
     */
    public void setRootColor(Pane pane, Color r, Image image) {
        setRootColor(pane, r, /* previousImageArt */ art != null ? art.getPreviousImage() : image, image);
    }
    /**
     * {@link #setRootColor(Pane, Color, Image)} с явным previous/current изображениями.
     * <p>
     * <b>Crossfade алгоритм:</b>
     * </p>
     * <ul>
     *   <li>background.opacity = 0 → 1 (fade in)</li>
     *   <li>background_under.opacity = 1 → 0 (fade out)</li>
     * </ul>
     * <p>Параллельная анимация длительностью {@code durationIn}.</p>
     */
    public void setRootColor(Pane pane, Color r, Image previous, Image image) {
        pane.setBackground(new Background(new BackgroundFill(r, CornerRadii.EMPTY, Insets.EMPTY)));

        background.setImage(image);
        background_under.setImage(previous);

        if(background_under.getImage() != null && background.getImage() != null) {
            background.setOpacity(0);
            background_under.setOpacity(1);

            Timeline fadeIn = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(background.opacityProperty(), 0)),
                    new KeyFrame(durationIn, new KeyValue(background.opacityProperty(), 1.0))
            );

            Timeline fadeOut = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(background_under.opacityProperty(), 1.0)),
                    new KeyFrame(durationIn, new KeyValue(background_under.opacityProperty(), 0))
            );

            ParallelTransition parallelTransition = new ParallelTransition(fadeIn, fadeOut);
            parallelTransition.play();
        }
    }
    /**
     * Создает Timeline для мгновенной установки цвета тени обложки.
     * <p>
     * <b>Особенности:</b>
     * </p>
     * <ul>
     *   <li>Длительность: {@code durationIn}</li>
     *   <li>Однократное выполнение (cycleCount=1)</li>
     *   <li>Без авто-повтора</li>
     * </ul>
     * <p>Используется в {@link #initColor} через {@code Animations.play()}.</p>
     *
     * @param c целевой цвет тени
     * @return Timeline объект
     */
    public Timeline dropShadowColor(Color c) {
        Timeline timeline = new Timeline(
                new KeyFrame(durationIn,
                        new KeyValue(imgTrackShadow.colorProperty(), c)
                )
        );

        timeline.setCycleCount(1);
        timeline.setAutoReverse(false);

        return timeline;
    }
    /**
     * Single-thread ExecutorService для анализа цветов изображений.
     * <p>
     * Гарантирует последовательную обработку обложек (одна за раз).
     * </p>
     */
    protected final ExecutorService service = Executors.newSingleThreadExecutor();
    /**
     * Инициализация цветовой схемы из обложки альбома (асинхронно).
     * <p>
     * <b>Алгоритм:</b>
     * </p>
     * <ul>
     *   <li>Background: ColorProcessor.core.getGeneralColorFromImage</li>
     *   <li>Thread-safe: imageLock + Platform.runLater</li>
     *   <li>UI обновления: dropShadow → colors → listViews → background → caption</li>
     * </ul>
     * <p>
     *   <b>Условные блоки:</b>
     * </p>
     * <ul>
     *   <li>"rainbow": updateColors + initListViews</li>
     *   <li>"is_blur_background": setRootColor crossfade</li>
     * </ul>
     */
    public void initColor(Image image) {
        service.submit(() -> {
            synchronized (imageLock) {
                Color newColor = ColorProcessor.core.getGeneralColorFromImage(image);

                Platform.runLater(() -> {
                    ColorProcessor.core.setMainClr(newColor);

                    Animations.instance.play(dropShadowColor(newColor), "dropShadowColor", () -> imgTrackShadow.setColor(newColor));

                    if (ConfigurationManager.instance.getBooleanItem("rainbow", "true")) {
                        updateColors(newColor);

                        initListViews();
                    }

                    if (ConfigurationManager.instance.getBooleanItem("is_blur_background", "true")) {
                        setRootColor(root, ColorProcessor.core.getMainClr(), image);
                    }

                    Root.rootImpl.setStageCaptionColor(stage, newColor);
                });
            }
        });
    }
    /**
     * Создает анимацию изменения размера DropShadow эффекта.
     * <p>
     * <b>Параметры анимации:</b>
     * </p>
     * <ul>
     *   <li>Анимирует <code>width</code> и <code>height</code> тени одновременно</li>
     *   <li>От текущих значений → <code>newProperties</code></li>
     *   <li>Длительность: <code>dura</code> миллисекунд</li>
     *   <li>Interpolator: произвольный</li>
     * </ul>
     *
     * @param node узел с DropShadow эффектом
     * @param dura длительность анимации (мс)
     * @param newProperties новое значение width/height тени
     * @param interpolator кривая ускорения
     * @return Timeline анимация
     */
    protected Animation dropShadowTimeLine(Node node, int dura, int newProperties, Interpolator interpolator) {
        DropShadow shadow = (DropShadow) node.getEffect();
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().setAll(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(shadow.widthProperty(), shadow.getWidth(), interpolator),
                        new KeyValue(shadow.heightProperty(), shadow.getHeight(), interpolator)
                ),
                new KeyFrame(Duration.millis(dura),
                        new KeyValue(shadow.widthProperty(), newProperties, interpolator),
                        new KeyValue(shadow.heightProperty(), newProperties, interpolator)
                )
        );

        return timeline;
    }
    /**
     * Обновляет иконку и заголовок окна Stage в зависимости от фокуса.
     * <p>
     * <b>Без фокуса:</b>
     * </p>
     * <ul>
     *   <li>Иконка: обложка альбома {@code art.getImage()}</li>
     *   <li>Заголовок: "Артист - Трек"</li>
     * </ul>
     * <p>
     *   <b>С фокусом:</b>
     * </p>
     * <ul>
     *   <li>Иконка: логотип приложения {@code logo}</li>
     *   <li>Заголовок: название приложения {@code Music.name}</li>
     * </ul>
     */
    public void setIcon() {
        stage.getIcons().setAll(!stage.isFocused() ? art.getImage() : logo);
        stage.setTitle(!stage.isFocused() ? currentArtist.getText() + " - " + currentTrackName.getText() : Music.name);
    }
    /**
     * {@inheritDoc}
     * <p>
     * Комплексная анимация смены обложки трека с Material Design эффектами.
     * </p>
     * <p><b>Этапы анимации:</b></p>
     * <ol>
     *   <li>Остановка всех активных анимаций</li>
     *   <li>Вычисление направления slide (горизонталь/вертикаль)</li>
     *   <li><b>outTransition:</b> Translate + FadeOut + ScaleOut</li>
     *   <li><b>inDropShadow:</b> тень → 0px (подготовка)</li>
     *   <li><b>outTransition.finished:</b> setImage → initColor → inTransition</li>
     *   <li><b>inTransition:</b> Translate + FadeIn + ScaleIn</li>
     *   <li><b>outDropShadow:</b> тень → size px</li>
     * </ol>
     */
    @Override
    public void initArt(Track track) {
        if (outTransition != null)
            outTransition.stop();

        if (inTransition != null)
            inTransition.stop();

        if (inDropShadowAnimation != null)
            inDropShadowAnimation.stop();

        if (outDropShadowAnimation != null)
            outDropShadowAnimation.stop();

        double width = art.getBoundsInParent().getWidth();
        double height = art.getBoundsInParent().getHeight();

        boolean goLeftOrDown = PlayProcessor.playProcessor.getPreviousIndex() < PlayProcessor.playProcessor.getTrackIter();

        double translateOutFromX;
        double translateOutFromY;
        double translateOutToX;
        double translateOutToY;

        double translateInFromX;
        double translateInFromY;
        double translateInToX;
        double translateInToY;

        // Горизонтальное направление
        if (!playlistSelected) {
            if (goLeftOrDown) {
                translateOutToX = -width;
                translateInFromX = width;
            } else {
                translateOutToX = width;
                translateInFromX = -width;
            }

            translateOutToY = 0;
            translateInFromY = 0;
        } else {
            // Вертикальное направление
            if (goLeftOrDown) {
                translateOutToY = -height;
                translateInFromY = height;
            } else {
                translateOutToY = height;
                translateInFromY = -height;
            }

            translateOutToX = 0;
            translateInFromX = 0;
        }

        translateOutFromX = 0;
        translateInToX = 0;
        translateOutFromY = 0;
        translateInToY = 0;

        TranslateTransition moveOut = new TranslateTransition(durationIn, art);
        moveOut.setFromX(translateOutFromX);
        moveOut.setToX(translateOutToX);
        moveOut.setFromY(translateOutFromY);
        moveOut.setToY(translateOutToY);
        moveOut.setInterpolator(acceleratingInterpolator);

        Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(art.opacityProperty(), 1, acceleratingInterpolator)),
                new KeyFrame(durationIn, new KeyValue(art.opacityProperty(), 0, acceleratingInterpolator))
        );

        Timeline scaleOut = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(art.scaleXProperty(), 1, acceleratingInterpolator),
                        new KeyValue(art.scaleYProperty(), 1, acceleratingInterpolator)),
                new KeyFrame(durationIn,
                        new KeyValue(art.scaleXProperty(), scaleOutTarget, acceleratingInterpolator),
                        new KeyValue(art.scaleYProperty(), scaleOutTarget, acceleratingInterpolator))
        );

        outTransition = new ParallelTransition(moveOut, fadeOut, scaleOut);

        inDropShadowAnimation = dropShadowTimeLine(
                art,
                250,
                0,
                general_interpolator
        );
        Animations.instance.play(inDropShadowAnimation, "inDropShadowAnimation");

        final double finalTranslateInFromY = translateInFromY;
        final double finalTranslateInFromX = translateInFromX;
        final double finalTranslateInToX = translateInToX;
        final double finalTranslateInToY = translateInToY;

        outTransition.setOnFinished(actionEvent -> new Thread(() -> {
            synchronized (imageLock) {
                Image img = track.getAlbumArt();

                Platform.runLater(() -> {
                    setImage(img);
                    initColor(art.getImage());
                    setIcon();

                    art.setTranslateX(finalTranslateInFromX);
                    art.setTranslateY(finalTranslateInFromY);
                    art.setOpacity(0);
                    art.setScaleX(scaleInStart);
                    art.setScaleY(scaleInStart);

                    outDropShadowAnimation = dropShadowTimeLine(
                            art,
                            250,
                            size,
                            general_interpolator
                    );

                    Animations.instance.play(outDropShadowAnimation, "outDropShadowAnimation", () -> {
                        if(art.getEffect() instanceof DropShadow dropShadow) {
                            dropShadow.setWidth(size);
                            dropShadow.setHeight(size);
                        }
                    });

                    TranslateTransition moveIn = new TranslateTransition(durationOut, art);
                    moveIn.setFromX(finalTranslateInFromX);
                    moveIn.setToX(finalTranslateInToX);
                    moveIn.setFromY(finalTranslateInFromY);
                    moveIn.setToY(finalTranslateInToY);
                    moveIn.setInterpolator(acceleratingInterpolator);

                    FadeTransition fadeIn = new FadeTransition(durationOut, art);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.setInterpolator(acceleratingInterpolator);

                    Timeline scaleIn = new Timeline(
                            new KeyFrame(Duration.ZERO,
                                    new KeyValue(art.scaleXProperty(), scaleInStart, acceleratingInterpolator),
                                    new KeyValue(art.scaleYProperty(), scaleInStart, acceleratingInterpolator)),
                            new KeyFrame(durationOut,
                                    new KeyValue(art.scaleXProperty(), scaleInEnd, acceleratingInterpolator),
                                    new KeyValue(art.scaleYProperty(), scaleInEnd, acceleratingInterpolator))
                    );

                    inTransition = new ParallelTransition(moveIn, fadeIn, scaleIn);

                    Animations.instance.play(inTransition, "inTransition", () -> {
                        art.setTranslateX(0);
                        art.setTranslateY(0);
                        art.setOpacity(1);
                        art.setScaleX(scaleInEnd);
                        art.setScaleY(scaleInEnd);
                    });
                });
            }
        }).start());

        if (playlistSelected)
            playlistSelected = false;

        Animations.instance.play(outTransition, "outTransition", () -> new Thread(() -> {
            synchronized (imageLock) {
                Image img = track.getAlbumArt();

                Platform.runLater(() -> {
                    art.setTranslateX(0);
                    art.setTranslateY(0);
                    art.setOpacity(1);
                    art.setScaleX(scaleInEnd);
                    art.setScaleY(scaleInEnd);

                    setImage(img);
                    initColor(art.getImage());
                    setIcon();
                });
            }
        }).start());
    }
    /**
     * Устанавливает новое изображение обложки с опциональной цветокоррекцией.
     * <p>
     * <b>Алгоритм:</b>
     * </p>
     * <ul>
     *   <li>Если <code>core.getHue() != 0</code> и <code>"is_hue_change" = true</code>:
     *       применяет {@link ColorProcessor#changeHue} с текущим hue значением</li>
     *   <li>Иначе: использует оригинальное изображение</li>
     * </ul>
     * <p>Обновляет {@code art.setImage(img)}.</p>
     *
     * @param img оригинальное изображение обложки
     */
    public void setImage(Image img) {
        if (ColorProcessor.core.getHue() != 0)
            img = ColorProcessor.core.changeHue(img, ConfigurationManager.instance.getBooleanItem("is_hue_change", "true") ? ColorProcessor.core.getHue() : 0);

        art.setImage(img);
    }
    /**
     * Обновляет цветовые схемы всех PlayView в root.
     * <p>
     * <b>Итерация:</b> перебор всех дочерних Node root.getChildren()
     * </p>
     * <p><b>Обновляемые элементы:</b></p>
     * <ul>
     *   <li><code>TrackListView:</code> selectedBackground + borderColor</li>
     *   <li><code>PlaylistListView:</code> selectedBackground + borderColor</li>
     * </ul>
     * <p>Цвет берется из <code>core.getMainClr()</code>.</p>
     *
     * <p>Вызывается из {@link #initColor} при <code>"rainbow" = true</code>.</p>
     */
    public void initListViews() {
          for(Node n : root.getChildren()) {
              if(n instanceof PlayView<?, ?> listView) {
                  listView.getTrackListView().updateSelectedBackground(core.getMainClr());
                  listView.getTrackListView().updateBorderColor(core.getMainClr());

                  listView.getPlaylistListView().updateSelectedBackground(core.getMainClr());
                  listView.getPlaylistListView().updateBorderColor(core.getMainClr());
              }
          }
    }

    /**
     * Фиксированный список парсеров обложек альбомов на основе {@link AlbumArtParser} enum.
     * <p>
     * <b>Преимущества enum-подхода:</b>
     * <ul>
     * <li>Типобезопасность и иммутабельность реализаций</li>
     * <li>Параллельное выполнение через {@link #parseImage(String, int, int, boolean, boolean)}</li>
     * <li>Автоматическое создание через {@code List.of(AlbumArtParser.values())}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Расширение:</b> Добавить новый парсер = новая константа в {@link AlbumArtParser}.
     * </p>
     *
     * @see AlbumArtParser
     * @see #parseImage(String, int, int, boolean, boolean)
     * @see #getInterfacesOfParseAlbumArtList()
     * @implNote Размер = {@link AlbumArtParser#values().length}
     */
    protected final List<IParseAlbumArt> iParseAlbumArts = new ArrayList<>(List.of(AlbumArtParser.values()));
    /**
     * Реализации парсеров обложек альбомов для параллельного выполнения.
     * <p>
     * <b>Архитектура:</b> Каждый enum - независимый парсер с fallback на {@link ColorProcessor#getLogo()}.
     * Выполняются параллельно в {@link #parseImage(String, int, int, boolean, boolean)} с таймаутом 2 сек.
     * </p>
     * <p>
     * <b>Расширение:</b> Новый парсер = новая константа enum + реализация {@link #getAlbumArt(String, int, int, boolean, boolean)}.
     * </p>
     *
     * <h3>Текущие парсеры</h3>
     * <dl>
     *   <dt>{@link #SPOTIFY}</dt>
     *   <dd>Spotify API через {@code me.API.Info.info.search()}.</dd>
     * </dl>
     *
     * @see ArtProcessor#iParseAlbumArts
     * @see #getAlbumArt(String, int, int, boolean, boolean)
     */
    public enum AlbumArtParser
            implements IParseAlbumArt
    {
        /**
         * Spotify API парсер обложек.
         * <p>
         * <b>Логика:</b>
         * <ol>
         *   <li>Поиск по {@code view} через {@code me.API.Info.info.search()}</li>
         *   <li>Извлечение URL из {@code getAwesomeAlbumArt().getUrl()}</li>
         *   <li>Загрузка {@link Image} или fallback на logo</li>
         * </ol>
         * </p>
         *
         * @implNote Обёрнуто в {@code RuntimeException} для прерывания медленных запросов
         */
        SPOTIFY {
            @Override
            public Image getAlbumArt(String view, int height, int width, boolean preserve, boolean smooth) {
                String url;

                try {
                    url = me.API.Info.info.search(URLEncoder.encode(view, StandardCharsets.UTF_8)).getAwesomeAlbumArt().getUrl();
                } catch (IOException | ParseException e) {
                    throw new RuntimeException(e);
                }

                if(url != null && !url.isEmpty()) {
                    return new Image(url, height, width, isPreserveRatio, isSmooth);
                }

                return ColorProcessor.core.getLogo();
            }
        };
    }

    /**
     * Возвращает список доступных парсеров обложек альбомов.
     * <p>
     * Предназначен для <b>только чтения</b>, либо для явного модифицирования. Позволяет инспектировать доступные провайдеры
     * без возможности изменения логики параллельного парсинга.
     * </p>
     *
     * <h3>Использование</h3>
     * <pre>{@code
     * List<IParseAlbumArt> parsers = getInterfacesOfParseAlbumArtList();
     * System.out.println("Доступно парсеров: " + parsers.size());
     * // НЕ изменяйте список!
     * }</pre>
     *
     * @return список реализаций {@link IParseAlbumArt}
     * @see #iParseAlbumArts
     * @see #parseImage(String, int, int, boolean, boolean)
     */
    public List<IParseAlbumArt> getInterfacesOfParseAlbumArtList() {
        return iParseAlbumArts;
    }
    /**
     * Параллельно пытается получить обложку альбома через все доступные парсеры.
     * <p>
     * <b>Детальная логика работы (пошагово):</b>
     * </p>
     *
     * <h4>1. Подготовка пула потоков</h4>
     * <pre>ExecutorService pool = newFixedThreadPool(PARSE_ALBUM_ARTS.size())</pre>
     * Создаётся пул потоков = количество парсеров (например, 3 потока для 3 парсеров).
     *
     * <h4>2. Создание задач</h4>
     * <pre>List&lt;Callable&lt;Image&gt;&gt; tasks = [...]</pre>
     * Для каждого парсера создаётся задача: {@code () -> parser.getAlbumArt(...)}.
     *
     * <h4>3. Параллельный запуск + таймаут</h4>
     * <pre>invokeAll(tasks, 2, SECONDS)</pre>
     * <b>КРИТИЧНО:</b> Все задачи запускаются ОДНОВРЕМЕННО и ждут <b>максимум 2 сек</b>.
     * - Быстрый парсер (0.3с) → завершается
     * - Медленный парсер (10с) → обрезается через 2с
     *
     * <h4>4. Поиск первого успеха</h4>
     * <pre>for (Future&lt;Image&gt; f : futures) {
     *     if (f.isDone() && !f.isCancelled()) {
     *         Image result = f.get();
     *         if (result != null) return result;  ← ПЕРВЫЙ НЕ-null!
     *     }
     * }</pre>
     * Проверяются futures <b>по порядку создания</b>. Первый успешный (не null) → немедленный возврат.
     *
     * <h4>5. Fallback</h4>
     * Если все парсеры провалились/таймаут → возвращает {@link ColorProcessor#logo}.
     *
     * <h3>Пример сценария</h3>
     * <pre>{@code
     * Парсеры: [DEEZER, LOCAL, FALLBACK]
     * Время ответа: [1.2с, 0.4с, 3.0с]
     *
     * 0.0с - все 3 запускаются параллельно
     * 0.4с - LOCAL завершился → ВОЗВРАЩАЕТСЯ (игнорируем остальные!)
     * DEEZER/FALLBACK обрезаются неиспользованными
     * }</pre>
     *
     * <h4>Обработка ошибок</h4>
     * <ul>
     * <li>{@code InterruptedException} - поток прерван</li>
     * <li>{@code ExecutionException} - парсер выбросил исключение</li>
     * </ul>
     * Оба случая → <b>молча возвращает logo</b>.
     *
     * @param view строка для поиска (название трека/исполнителя)
     * @param height высота изображения
     * @param width ширина изображения
     * @param isPreserveRation сохранять пропорции
     * @param isSmooth сглаживание
     * @return первая успешная обложка или {@link ColorProcessor#logo}
     * @see AlbumArtParser
     * @see ArtProcessor#iParseAlbumArts
     */
    public Image parseImage(String view, int height, int width, boolean isPreserveRation, boolean isSmooth) {
        try(ExecutorService executor = Executors.newFixedThreadPool(iParseAlbumArts.size())) {
            List<Callable<Image>> tasks = new ArrayList<>();
            for (rf.ebanina.Network.IParseAlbumArt func : iParseAlbumArts) {
                tasks.add(() -> func.getAlbumArt(view, height, width, isPreserveRation, isSmooth));
            }

            List<Future<Image>> futures = executor.invokeAll(tasks, ConfigurationManager.instance.getIntItem("network_art_parse_timeout", "2"), TimeUnit.SECONDS);

            for (Future<Image> f : futures) {
                if (f.isDone() && !f.isCancelled()) {
                    Image result = f.get();

                    if (result != null) {
                        executor.shutdownNow();
                        return result;
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            return logo;
        }

        return logo;
    }
    /**
     * Параллельный парсинг обложки с настройками по умолчанию.
     * <p>
     * <b>Параметры по умолчанию:</b><br>
     * {@code isPreserveRatio = ColorProcessor.isPreserveRatio}<br>
     * {@code isSmooth = ColorProcessor.isSmooth}
     * </p>
     *
     * @param view строка для поиска
     * @param height высота изображения
     * @param width ширина изображения
     * @return первая успешная обложка или {@link ColorProcessor#logo}
     * @see #parseImage(String, int, int, boolean, boolean)
     * @see ColorProcessor#isPreserveRatio
     * @see ColorProcessor#isSmooth
     */
    public Image parseImage(String view, int height, int width) {
        return parseImage(view, height, width, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth);
    }
    /**
     * Параллельный парсинг обложки с параметрами по умолчанию.
     * <p>
     * <b>Параметры по умолчанию:</b><br>
     * {@code height = ColorProcessor.size}<br>
     * {@code width = ColorProcessor.size}<br>
     * {@code isPreserveRatio = ColorProcessor.isPreserveRatio}<br>
     * {@code isSmooth = ColorProcessor.isSmooth}
     * </p>
     *
     * <h3>Типичное использование</h3>
     * <pre>{@code
     * Image cover = processor.parseImage("The Weeknd - Blinding Lights");
     * // 128x128, preserveRatio=true, smooth=true (стандарт UI)
     * }</pre>
     *
     * @param view строка для поиска (название/исполнитель)
     * @return обложка стандартного размера или {@link ColorProcessor#logo}
     * @see #parseImage(String, int, int, boolean, boolean)
     * @see ColorProcessor#size
     */
    public Image parseImage(String view) {
        return parseImage(view, ColorProcessor.size, ColorProcessor.size);
    }
}