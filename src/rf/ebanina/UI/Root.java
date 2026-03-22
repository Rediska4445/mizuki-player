package rf.ebanina.UI;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import javafx.animation.*;
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
import javafx.scene.control.MultipleSelectionModel;
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
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import rf.ebanina.File.Configuration.ConfigurableField;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Field;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Localization.Locales;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.Network.Info;
import rf.ebanina.UI.Editors.Metadata.Track.Metadata;
import rf.ebanina.UI.UI.Context.Tooltip.ContextTooltip;
import rf.ebanina.UI.UI.Element.AnimationDialog;
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
import rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists.ListCellSimilar;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists.ListCellTrack;
import rf.ebanina.UI.UI.Element.ListViews.Playlist.PlayView;
import rf.ebanina.UI.UI.Element.Slider.SoundSlider;
import rf.ebanina.UI.UI.Element.Text.TextField;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.UI.UI.Popup.LabelPopupMenu;
import rf.ebanina.UI.UI.Popup.PreviewPopupService;
import rf.ebanina.ebanina.KeyBindings.Keys;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.ArtProcessor;
import rf.ebanina.ebanina.Player.Controllers.IArtProcessor;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static rf.ebanina.File.Resources.ResourceManager.BIN_LIBRARIES_PATH;
import static rf.ebanina.Network.Info.updateSimilarListAsync;
import static rf.ebanina.UI.Root.PlaylistHandler.playlistSelected;
import static rf.ebanina.ebanina.KeyBindings.KeyBindingController.isKeyPressed;
import static rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor.playProcessor;

@logging(tag = "root")
public class Root
        implements IRoot
{
    public static final Map<String, Map.Entry<Point, Dimension>> windowsSizes = new WeakHashMap<>();

    public static native void setCaptionColor(long wid, int color);
    public static native void addJumpListTask(long wid, int color);

    public static Root rootImpl = new Root();

    public static final String OS = System.getProperty("os.name");

    public static Stage stage;

    @ConfigurableField(key = "album_art_corners", ifNull = "15")
    public static int corners;

    public static Pane root;

    public Scene scene;
    public static SoundSlider soundSlider;
    public static MultipleSelectionModel<Track> trackSelectionModel;
    public static MultipleSelectionModel<Playlist> playlistSelectionModel;

    public static Art art;

    public static PlayView<Track, Playlist> tracksListView;

    public static PlayView<Track, Playlist> similar;

    public static TextField currentTrackName;
    public static TextField currentArtist;
    public static TextField beginTime;
    public static TextField endTime;

    public static rf.ebanina.UI.UI.Element.Buttons.Button hideControlRight;
    public static rf.ebanina.UI.UI.Element.Buttons.Button hideControlLeft;
    public static rf.ebanina.UI.UI.Element.Buttons.Button tracksHistory;

    public static BorderPane topDataPane;

    public static DropShadow imgTrackShadow = new DropShadow();

    public static ImageView background = new ImageView();
    public static ImageView background_under = new ImageView();

    public static Interpolator general_interpolator;

    public static final class Layout {
        private float imgTop = 125;
        private float imgBottom = 35;
        private float maxListViewSize = 100;

        private int paddingArt = 35;

        private float trackListViewHeightSubtract = 100;

        public float getTrackListViewHeightSubtract() {
            return trackListViewHeightSubtract;
        }

        public Layout setTrackListViewHeightSubtract(float trackListViewHeightSubtract) {
            this.trackListViewHeightSubtract = trackListViewHeightSubtract;
            return this;
        }

        private float similarWidthSubtract = 35;

        public float getSimilarWidthSubtract() {
            return similarWidthSubtract;
        }

        public Layout setSimilarWidthSubtract(float similarWidthSubtract) {
            this.similarWidthSubtract = similarWidthSubtract;
            return this;
        }

        private float similarLayoutXPadding = 25;

        public float getSimilarLayoutXPadding() {
            return similarLayoutXPadding;
        }

        public Layout setSimilarLayoutXPadding(float similarLayoutXPadding) {
            this.similarLayoutXPadding = similarLayoutXPadding;
            return this;
        }

        private float trackListViewToImgTrackArtRoundXSubtract = 50;

        public float getTrackListViewToImgTrackArtRoundXSubtract() {
            return trackListViewToImgTrackArtRoundXSubtract;
        }

        public Layout setTrackListViewToImgTrackArtRoundXSubtract(float trackListViewToImgTrackArtRoundXSubtract) {
            this.trackListViewToImgTrackArtRoundXSubtract = trackListViewToImgTrackArtRoundXSubtract;
            return this;
        }

        private float hideControlLeftToImgTrackArtHeight = 0.5f;

        public float getHideControlLeftToImgTrackArtHeight() {
            return hideControlLeftToImgTrackArtHeight;
        }

        public Layout setHideControlLeftToImgTrackArtHeight(float hideControlLeftToImgTrackArtHeight) {
            this.hideControlLeftToImgTrackArtHeight = hideControlLeftToImgTrackArtHeight;
            return this;
        }

        private float hideControlLeftLayoutX = 2;

        public float getHideControlLeftLayoutX() {
            return hideControlLeftLayoutX;
        }

        public Layout setHideControlLeft(float hideControlLeftLayoutX) {
            this.hideControlLeftLayoutX = hideControlLeftLayoutX;
            return this;
        }

        private float hideControlRightToImgTrackArtHeightMultiplier = 0.5f;

        public float getHideControlRightToImgTrackArtHeightMultiplier() {
            return hideControlRightToImgTrackArtHeightMultiplier;
        }

        public Layout setHideControlRightToImgTrackArtHeightMultiplier(float hideControlRightToImgTrackArtHeightMultiplier) {
            this.hideControlRightToImgTrackArtHeightMultiplier = hideControlRightToImgTrackArtHeightMultiplier;
            return this;
        }

        private float btnSliderWidthMultiplier = 0.5f;

        public float getBtnSliderWidthMultiplier() {
            return btnSliderWidthMultiplier;
        }

        public Layout setBtnSliderWidthMultiplier(float btnSliderWidthMultiplier) {
            this.btnSliderWidthMultiplier = btnSliderWidthMultiplier;
            return this;
        }

        private float sliderBlurBackgroundBeginTimeMultiplier = 1.5f;

        public float getSliderBlurBackgroundBeginTimeMultiplier() {
            return sliderBlurBackgroundBeginTimeMultiplier;
        }

        public Layout setSliderBlurBackgroundBeginTimeMultiplier(float sliderBlurBackgroundBeginTimeMultiplier) {
            this.sliderBlurBackgroundBeginTimeMultiplier = sliderBlurBackgroundBeginTimeMultiplier;
            return this;
        }

        private float sliderBlurBackgroundWidthAdd = 20;

        public float getSliderBlurBackgroundWidthAdd() {
            return sliderBlurBackgroundWidthAdd;
        }

        public Layout setSliderBlurBackgroundWidthAdd(float sliderBlurBackgroundWidthAdd) {
            this.sliderBlurBackgroundWidthAdd = sliderBlurBackgroundWidthAdd;
            return this;
        }

        private float sliderBlurBackgroundHeightAdd = 20;

        public float getSliderBlurBackgroundHeightAdd() {
            return sliderBlurBackgroundHeightAdd;
        }

        public Layout setSliderBlurBackgroundHeightAdd(float sliderBlurBackgroundHeightAdd) {
            this.sliderBlurBackgroundHeightAdd = sliderBlurBackgroundHeightAdd;
            return this;
        }

        private float sliderBlurBackgroundYSubtract = 10;

        public float getSliderBlurBackgroundYSubtract() {
            return sliderBlurBackgroundYSubtract;
        }

        public Layout setSliderBlurBackgroundYSubtract(float sliderBlurBackgroundYSubtract) {
            this.sliderBlurBackgroundYSubtract = sliderBlurBackgroundYSubtract;
            return this;
        }

        private float sliderBlurBackgroundXSubtract = 10;

        public float getSliderBlurBackgroundXSubtract() {
            return sliderBlurBackgroundXSubtract;
        }

        public Layout setSliderBlurBackgroundXSubtract(float sliderBlurBackgroundXSubtract) {
            this.sliderBlurBackgroundXSubtract = sliderBlurBackgroundXSubtract;
            return this;
        }

        private float topDataPaneWidthXMultiplier = 0.5f;

        public float getTopDataPaneWidthXMultiplier() {
            return topDataPaneWidthXMultiplier;
        }

        public Layout setTopDataPaneWidthXMultiplier(float topDataPaneWidthXMultiplier) {
            this.topDataPaneWidthXMultiplier = topDataPaneWidthXMultiplier;
            return this;
        }

        private float imgTrackArtRoundWidthYMultiplier = 0.8f;

        public float getImgTrackArtRoundWidthYMultiplier() {
            return imgTrackArtRoundWidthYMultiplier;
        }

        public Layout setImgTrackArtRoundWidthYMultiplier(float imgTrackArtRoundWidthYMultiplier) {
            this.imgTrackArtRoundWidthYMultiplier = imgTrackArtRoundWidthYMultiplier;
            return this;
        }

        private float imgTrackArtRoundYMultiplier = 0.5f;

        public float getImgTrackArtRoundYMultiplier() {
            return imgTrackArtRoundYMultiplier;
        }

        public Layout setImgTrackArtRoundYMultiplier(float imgTrackArtRoundYMultiplier) {
            this.imgTrackArtRoundYMultiplier = imgTrackArtRoundYMultiplier;
            return this;
        }

        private float imgTrackArtRoundWidthXMultiplier = 0.5f;

        public float getImgTrackArtRoundWidthXMultiplier() {
            return imgTrackArtRoundWidthXMultiplier;
        }

        public Layout setImgTrackArtRoundWidthXMultiplier(float imgTrackArtRoundWidthXMultiplier) {
            this.imgTrackArtRoundWidthXMultiplier = imgTrackArtRoundWidthXMultiplier;
            return this;
        }

        private float imgTrackArtRoundXMultiplier = 0.5f;

        public float getImgTrackArtRoundXMultiplier() {
            return imgTrackArtRoundXMultiplier;
        }

        public Layout setImgTrackArtRoundXMultiplier(float imgTrackArtRoundXMultiplier) {
            this.imgTrackArtRoundXMultiplier = imgTrackArtRoundXMultiplier;
            return this;
        }

        private int sliderBottom = 50;

        private int hideControlRightSide = 18;

        private int bottomTracklistBottom = 65;

        private int currentTrackTopPlaylistPane = 65;

        public int getCurrentTrackTopPlaylistPane() {
            return currentTrackTopPlaylistPane;
        }

        public Layout setCurrentTrackTopPlaylistPane(int currentTrackTopPlaylistPane) {
            this.currentTrackTopPlaylistPane = currentTrackTopPlaylistPane;
            return this;
        }

        public int getHideControlRightSide() {
            return hideControlRightSide;
        }

        public Layout setHideControlRightSide(int hideControlRightSide) {
            this.hideControlRightSide = hideControlRightSide;
            return this;
        }

        public int getBottomTracklistBottom() {
            return bottomTracklistBottom;
        }

        public Layout setBottomTracklistBottom(int bottomTracklistBottom) {
            this.bottomTracklistBottom = bottomTracklistBottom;
            return this;
        }

        public Layout() {}

        public int getSliderBottom() {
            return sliderBottom;
        }

        public Layout setSliderBottom(int sliderBottom) {
            this.sliderBottom = sliderBottom;
            return this;
        }

        public int getPaddingArt() {
            return paddingArt;
        }

        public Layout setPaddingArt(int paddingArt) {
            this.paddingArt = paddingArt;
            return this;
        }

        public float getMaxListViewSize() {
            return maxListViewSize;
        }

        public Layout setMaxListViewSize(float maxListViewSize) {
            this.maxListViewSize = maxListViewSize;
            return this;
        }

        public float getImgTop() {
            return imgTop;
        }

        public Layout setImgTop(float imgTop) {
            this.imgTop = imgTop;
            return this;
        }

        public float getImgBottom() {
            return imgBottom;
        }

        public Layout setImgBottom(float imgBottom) {
            this.imgBottom = imgBottom;
            return this;
        }
    }

    public static final Layout rootLayout = new Layout();

    public static NextButton btnNext;
    public static PlayButton btn;
    public static PrevButton btnDown;

    public static ControlPane mainFunctions;

    public static javafx.scene.shape.Rectangle sliderBlurBackground = new javafx.scene.shape.Rectangle();

    public static IArtProcessor artProcessor = new ArtProcessor();

    public static double ICE_FRICTION = 14.0;

    public static Interpolator iceInterpolator = new Interpolator() {
        @Override
        protected double curve(double t) {
            return (t == 1.0) ? 1.0 : 1.0 - Math.pow(2.0, -ICE_FRICTION * t);
        }
    };

    private static void loadDwmApiLibrary(String path) {
        File file = new File(path);

        if (!file.exists()) {
            throw new UnsatisfiedLinkError("Библиотека не найдена по пути: " + path);
        }

        System.load(file.getAbsolutePath());
    }

    public static void setStageCaptionColor(Stage stage, Color color) {
        if (ConfigurationManager.instance.getBooleanItem("album_art_caption_paint", "true") && OS.contains("Windows 11")) {
            long wid = getNativeHandlePeerForStage(stage);

            int res = Integer.parseInt(convertColorTo16(color), 16);

            // dwmApi.getFunction("Java_App_setCaptionColor").invoke(new Object[]{wid, res});

            setCaptionColor(wid, res);
        }
    }

    private static String convertColorTo16(Color color) {
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

    public static long getNativeHandlePeerForStage(Stage stage) {
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

    public void alert(String title, String msg, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }

    public static void showError(String title, String msg) {
        rootImpl.alert(title, msg, Alert.AlertType.ERROR);
    }

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

        trackSelectionModel.select(PlayProcessor.playProcessor.getTrackIter());

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
                Metadata.getInstance().prepare(PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()));
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
        soundSlider.setColor(Color.BLACK);
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

        root.getChildren().add(Root.hideControlRight);
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
            AnimationDialog agreementDialog = new AnimationDialog(
                    stage, root
            );

            VBox content = new VBox(20);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(30));

            rf.ebanina.UI.UI.Element.Text.Label title = new rf.ebanina.UI.UI.Element.Text.Label("Пользовательское соглашение");
            title.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");

            Text terms = new Text("Здесь должен быть очень длинный и важный текст " +
                    "о том, что пользователь согласен на всё...");
            terms.setFill(Color.LIGHTGRAY);
            terms.setWrappingWidth(400);

            javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("ПРИНЯТЬ");

            content.getChildren().addAll(title, terms, closeBtn);

            agreementDialog.getChildren().add(content);
            agreementDialog.setDialogMaxSize(0.7, 0.85);
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

            root.getChildren().add(agreementDialog);

            agreementDialog.prefHeightProperty().bind(stage.heightProperty());
            agreementDialog.prefWidthProperty().bind(stage.widthProperty());

            agreementDialog.show();
        }
    }

    private Dimension beforePortable = new Dimension();

    private AtomicBoolean isInternalMaximazied = new AtomicBoolean(false);
    private AtomicBoolean isMaximazied = new AtomicBoolean();

    public static Animation Blur(Node node, int dura, int newProperties) {
        GaussianBlur blur = (GaussianBlur) node.getEffect();
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().setAll(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(blur.radiusProperty(), blur.getRadius())),
                new KeyFrame(Duration.millis(dura),
                        new KeyValue(blur.radiusProperty(), newProperties))
        );

        return timeline;
    }

    public static Runnable initBinds = () -> {
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

    private static String readAllLicensesRecursively(String baseDir, String langCode) {
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

    // FIXME: Не всегда правильно отслеживает
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

    public Pane getRoot() {
        return Objects.requireNonNullElseGet(root, () -> root = new Pane());
    }

    public WeakConst<Long> HWND = new WeakConst<>();

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

    public static Runnable onSet = null;

    private static ImageView createBackground(ImageView background) {
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

    public void nodesIter(Consumer<Node> supplier, Node... a) {
        for(Node n : a)
            supplier.accept(n);
    }

    public void toFront(Node... a) {
        nodesIter(Node::toFront, a);
    }

    public void toBack(Node... a) {
        nodesIter(Node::toBack, a);
    }

    private static void resizeBackground(ImageView background) {
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

    public static Runnable onInit = null;

    public static final ExecutorService rootExecService = Executors.newCachedThreadPool();

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

    public void init() {
        root = getRoot();

        mainFunctions = new ControlPane(root);

        rootExecService.submit(() -> {
            rf.ebanina.UI.UI.Context.Menu.ContextMenu trackHistoryContextMenu = new rf.ebanina.UI.UI.Context.Menu.ContextMenu();
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

        trackSelectionModel = tracksListView.getTrackListView().getSelectionModel();
        trackSelectionModel.setSelectionMode(SelectionMode.SINGLE);

        playlistSelectionModel = tracksListView.getPlaylistListView().getSelectionModel();
        playlistSelectionModel.setSelectionMode(SelectionMode.SINGLE);

        soundSlider = new SoundSlider(0, 120, 0);

        topDataPane = new BorderPane();

        if(onInit != null)
            onInit.run();
    }

    private final LonelyThreadPool loadSlider = new LonelyThreadPool();

    public void loadRectangleOfGainVolumeSlider(File file) {
        loadSlider.runNewTask(() -> {
            soundSlider.loadSliderBackground(file);
        });
    }

    public void openBrowser(URI url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ProcessBuilder openInExplorer(String path) {
        return new ProcessBuilder("explorer", "/select,", path);
    }

    public static class SliderHandler {
        public static Cursor cursor = Cursor.NONE;
        private static boolean worry;
        public static final int sleep = ConfigurationManager.instance.getIntItem("slider_thread_sleep", "1000");

        public static void initialize() {
            soundSlider.setOnMousePressed((EventHandler<Event>) event -> {
                worry = true;

                soundSlider.setCursor(cursor);
            });

            soundSlider.setOnMouseReleased((EventHandler<Event>) event -> {
                worry = false;

                if (MediaProcessor.mediaProcessor.mediaPlayer == null || soundSlider == null)
                    return;

                if (((MouseEvent) event).getButton() == MouseButton.PRIMARY) {
                    MediaProcessor.mediaProcessor.setCurrentTime(Duration.seconds(soundSlider.getValue()));
                } else if(((MouseEvent) event).getButton() == MouseButton.SECONDARY) {
                    String path = Resources.Properties.DEFAULT_CACHE_TRACKS_PATH.getKey()
                            + File.separator
                            + FileManager.instance.name(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getFileName());

                    if(!isKeyPressed(NativeKeyEvent.VC_SHIFT)) {
                        FileManager.instance.save(path,
                                PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()).toString(),
                                Field.DataTypes.LIKE_MOMENT_START.code,
                                String.valueOf(soundSlider.getValue()));

                        MediaProcessor.mediaProcessor.mediaPlayer.setStartTime(Duration.seconds(Double.parseDouble(String.valueOf(soundSlider.getValue()))));

                        LabelPopupMenu l = new LabelPopupMenu("Begin of like moment set to " + soundSlider.getValue());
                        l.getLabel().setPrefWidth(100);
                        l.getLabel().setFont(ResourceManager.Instance.loadFont("main_font", 28));
                        l.ShowHide(topDataPane, Duration.seconds(1));
                    } else {
                        FileManager.instance.save(path,
                                PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()).toString(),
                                Field.DataTypes.LIKE_MOMENT_STOP.code,
                                String.valueOf(soundSlider.getValue()));

                        MediaProcessor.mediaProcessor.mediaPlayer.setStopTime(Duration.seconds(Double.parseDouble(String.valueOf(soundSlider.getValue()))));

                        LabelPopupMenu l = new LabelPopupMenu("End of like moment set to " + Track.getFormattedTotalDuration((int) soundSlider.getValue()));
                        l.getLabel().setPrefWidth(100);
                        l.getLabel().setFont(ResourceManager.Instance.loadFont("main_font", 28));
                        l.ShowHide(topDataPane, Duration.seconds(1));
                    }
                }

                soundSlider.setCursor(Cursor.DEFAULT);
            });

            soundSlider.valueProperty().addListener((changed, oldValue, newValue) -> {
                Platform.runLater(() -> {
                    if(MediaProcessor.mediaProcessor.mediaPlayer != null) {
                        Root.beginTime.setText((Track.getFormattedTotalDuration(MediaProcessor.mediaProcessor.mediaPlayer.getCurrentTime().toSeconds())));
                    }
                });
            });

            thread.start();
        }

        private static volatile boolean isHandling = true;

        public static void stop() {
            isHandling = false;
        }

        public static Thread thread = new Thread(() -> {
            while (isHandling) {
                try {
                    if (!worry && MediaProcessor.mediaProcessor.mediaPlayer != null && soundSlider != null && soundSlider.getScene() != null) {
                        if (MediaProcessor.mediaProcessor.mediaPlayer.getStatus() == rf.ebanina.ebanina.Player.MediaPlayer.Status.PLAYING) {
                            Platform.runLater(() -> {
                                if (MediaProcessor.mediaProcessor.mediaPlayer != null && soundSlider != null) {
                                    soundSlider.setValue(MediaProcessor.mediaProcessor.mediaPlayer.getCurrentTime().toSeconds() / MediaProcessor.mediaProcessor.mediaPlayer.getTempo());
                                }
                            });
                        }
                    }

                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    isHandling = false;
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, "slider");
    }

    public static class ButtonHandler {
        public static void initialize() {
            hideControlRight.setOnMouseClicked((EventHandler<javafx.event.Event>) event -> {
                RotateTransition rt = new RotateTransition();
                rt.setNode(Root.hideControlRight.getGraphic());
                rt.setByAngle(180);
                rt.setDuration(Duration.millis(125));
                rt.play();
                rt.setOnFinished(event1 -> {
                    if(tracksListView.isOpened()) {
                        tracksListView.close();
                    } else {
                        tracksListView.open();
                    }
                });
            });

            hideControlLeft.setOnMouseClicked((EventHandler<javafx.event.Event>) event -> {
                RotateTransition rt = new RotateTransition();
                rt.setNode(Root.hideControlLeft.getGraphic());
                rt.setDuration(Duration.millis(125));
                rt.setByAngle(180);
                rt.play();
                rt.setOnFinished(actionEvent -> {
                    if(similar.isOpened()) {
                        similar.close();

                        Info.similarStop();
                    } else {
                        similar.open();

                        Info.similarStart();
                    }
                });
            });

            similar.getBtnPlaylist().setOnAction(event -> {
                similar.getTrackListView().setDisable(!similar.getTrackListView().isDisable());
                similar.getTrackListView().setVisible(!similar.getTrackListView().isVisible());

                PlaylistController.playlistController.onPlaylistChanged.run();
            });

            similar.getCurrentPlaylistText().setOnKeyReleased((e) -> {
                if(e.getCode() == KeyCode.ENTER) {
                    similar.getTrackListView().getItems().clear();
                    Root.PlaylistHandler.playlistSimilar.clear();

                    updateSimilarListAsync(similar.getCurrentPlaylistText().getText()).start();
                }
            });

            tracksListView.getBtnPlaylistNext().setOnMouseClicked((EventHandler<javafx.event.Event>) event -> {
                PlaylistController.playlistController.next();

                tracksListView.getCurrentPlaylistText().setText(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath());
            });

            tracksListView.getBtnPlaylistDown().setOnMouseClicked((EventHandler<javafx.event.Event>) event -> {
                PlaylistController.playlistController.down();

                tracksListView.getCurrentPlaylistText().setText(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath());
            });

            btn.setOnMouseClicked((EventHandler<javafx.event.Event>) event -> {
                MediaProcessor.mediaProcessor.pause_play();
            });

            tracksListView.getBtnPlaylist().setOnMouseClicked((event -> {
                if(event.getButton() == MouseButton.PRIMARY) {
                    tracksListView.getTrackListView().setDisable(!tracksListView.getTrackListView().isDisable());
                    tracksListView.getTrackListView().setVisible(!tracksListView.getTrackListView().isVisible());

                    PlaylistController.playlistController.onPlaylistChanged.run();
                } else if(event.getButton() == MouseButton.SECONDARY) {
                    try {
                        PlayProcessor.playProcessor.setCurrentMusicDir(String.valueOf(FileManager.instance.getFileFromOpenFileDialog(stage)));

                        PlayProcessor.playProcessor.getTracks().clear();
                        PlayProcessor.playProcessor.getTracks().addAll(FileManager.instance.getMusic(Paths.get(PlayProcessor.playProcessor.getCurrentMusicDir())));

                        tracksListView.getTrackListView().getItems().clear();
                        tracksListView.getTrackListView().getItems().addAll(PlayProcessor.playProcessor.getTracks());

                        tracksListView.getCurrentPlaylistText().setText(PlayProcessor.playProcessor.getCurrentMusicDir());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }));

            btnNext.setOnMouseClicked((EventHandler<javafx.event.Event>) event -> {
                if(isKeyPressed(NativeKeyEvent.VC_SHIFT) && isKeyPressed(NativeKeyEvent.VC_ALT)) {
                    PlaylistHandler.openTrack(PlayProcessor.playProcessor.getTrackHistoryGlobal().forward());
                } else if(isKeyPressed(NativeKeyEvent.VC_SHIFT) && !isKeyPressed(NativeKeyEvent.VC_ALT)) {
                    MediaProcessor.mediaProcessor.skipIntro(playProcessor.getTracks().get(playProcessor.getTrackIter()).getPath());
                } else if(!isKeyPressed(NativeKeyEvent.VC_SHIFT) && !isKeyPressed(NativeKeyEvent.VC_ALT)) {
                    PlayProcessor.playProcessor.next();
                }
            });

            btnDown.setOnMouseClicked((EventHandler<Event>) event -> {
                if(isKeyPressed(NativeKeyEvent.VC_SHIFT) && isKeyPressed(NativeKeyEvent.VC_ALT)) {
                    PlaylistHandler.openTrack(PlayProcessor.playProcessor.getTrackHistoryGlobal().back());
                } else if(isKeyPressed(NativeKeyEvent.VC_SHIFT) && !isKeyPressed(NativeKeyEvent.VC_ALT)) {
                    MediaProcessor.mediaProcessor.skipOutro(playProcessor.getTracks().get(playProcessor.getTrackIter()).getPath());
                } else if(!isKeyPressed(NativeKeyEvent.VC_SHIFT) && !isKeyPressed(NativeKeyEvent.VC_ALT)) {
                    PlayProcessor.playProcessor.down();
                }
            });
        }
    }

    private static class TrackListView {
        public static void set() {
            root.getChildren().add(tracksListView);

            tracksListView.getTrackListView().setCellFactory(lv -> new ListCellTrack<>());
            tracksListView.getPlaylistListView().setCellFactory(lv -> new ListCellPlaylist<>(ResourceManager.Instance.loadImage("playlistIcon",
                    40, 40, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth)));
            tracksListView.setPlayProcessor(PlayProcessor.playProcessor);
        }
    }

    private static class SimilarListView {
        public static void set() {
            root.getChildren().add(similar);

            similar.getTrackListView().setCellFactory(lv -> new ListCellSimilar());
            similar.getPlaylistListView().setCellFactory(lv -> new ListCellPlaylist<>(ResourceManager.Instance.loadImage("playlistIcon",
                    40, 40, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth)));
            similar.setPlayProcessor(PlayProcessor.playProcessor);
        }
    }

    public static class PlaylistHandler {
        public static boolean playlistExit = false;
        public static boolean playlistSelected = false;

        public static void initialize() {
            tracksListView.getTrackListView().setOnKeyPressed(keyEvent -> {
                Track selected = trackSelectionModel.getSelectedItem();

                if (keyEvent.getCode() == KeyCode.DELETE) {
                    if (selected != null) {
                        File file = new File(selected.getFilePath().toString());
                        Desktop.getDesktop().moveToTrash(file);
                    }

                    PlayProcessor.playProcessor.getTracks().remove(selected);

                    tracksListView.getTrackListView().getItems().remove(selected);
                    tracksListView.getTrackListView().getSelectionModel().select(PlayProcessor.playProcessor.getTrackIter());
                } else if(keyEvent.getCode() == KeyCode.BACK_SPACE) {
                    Root.tracksListView.getTrackListView().getItems().remove(selected);
                }
            });

            tracksListView.getTrackListView().setOnDragOver(event -> {
                if (event.getGestureSource() != root
                        && event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }

                event.consume();
            });

            tracksListView.getPlaylistListView().setOnMouseClicked((e) -> {
                if(e.getButton() == MouseButton.PRIMARY) {
                    Playlist n = tracksListView.getPlaylistListView().getSelectionModel().getSelectedItem();

                    try {
                        tracksListView.getTrackListView().getItems().clear();
                        tracksListView.getTrackListView().getItems().addAll(FileManager.instance.getMusic(Paths.get(n.getPath())));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }

                    tracksListView.openTrackList();
                    tracksListView.getCurrentPlaylistText().setText(n.getPath());

                    playlistExit = true;
                }
            });

            tracksListView.getTrackListView().setOnMouseClicked(e -> {
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
                    Track newValue = tracksListView.getTrackListView().getSelectionModel().getSelectedItem();

                    if (newValue != null
                            && !PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()).toString().equals(newValue.toString())
                            && PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()).compareTo(newValue) != 0) {
                        if(e.getButton() == MouseButton.PRIMARY) {
                            openTrack(newValue);
                        } else if(e.getButton() == MouseButton.MIDDLE) {
                            PlayProcessor.playProcessor.getTracks().clear();
                            PlayProcessor.playProcessor.getTracks().addAll(tracksListView.getTrackListView().getItems());
                        }
                    }
                }
            });

            tracksListView.getCurrentPlaylistText().setOnKeyReleased(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    if (Files.exists(Path.of(tracksListView.getCurrentPlaylistText().getText()))) {
                        PlaylistController.playlistController.setPlaylist(tracksListView.getCurrentPlaylistText().getText());
                    }
                }
            });

            similar.getTrackListView().setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && similar.getTrackListView().getFocusModel().getFocusedItem() != null) {
                    if (!PlayProcessor.playProcessor.isNetwork()) {
                        playlistSimilar.clear();
                        playlistSimilar.addAll(similar.getTrackListView().getItems());

                        PlayProcessor.playProcessor.getTracks().clear();
                        PlayProcessor.playProcessor.getTracks().addAll(playlistSimilar);

                        PlayProcessor.playProcessor.setNetwork(true);
                    }

                    PlayProcessor.playProcessor.setTrackIter(similar.getTrackListView().getSelectionModel().getSelectedIndex());

                    openTrack(similar.getTrackListView().getSelectionModel().getSelectedItem());
                }
            });

            tracksListView.getTrackListView().getItems().clear();
            tracksListView.getTrackListView().getItems().addAll(PlayProcessor.playProcessor.getTracks());

            tracksListView.getPlaylistListView().getItems().clear();
            tracksListView.getPlaylistListView().getItems().addAll(PlayProcessor.playProcessor.getCurrentPlaylist());

            tracksListView.getCurrentPlaylistText().setText(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath());
        }

        public static final ArrayList<Track> playlistSimilar = new ArrayList<>();

        public static void openTrack(Track newValue) {
            playlistSelected = true;

            PlayProcessor.playProcessor.open(newValue);

            if (playlistExit) {
                if(newValue.isNetty()) {
                    similar.getTrackListView().getSelectionModel().select(PlayProcessor.playProcessor.getTrackIter());
                } else {
                    tracksListView.getTrackListView().getSelectionModel().select(PlayProcessor.playProcessor.getTrackIter());
                }

                playlistExit = false;
            }
        }
    }
}