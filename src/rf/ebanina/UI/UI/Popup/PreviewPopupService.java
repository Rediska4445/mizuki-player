package rf.ebanina.UI.UI.Popup;

import javafx.application.Platform;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static rf.ebanina.UI.UI.Paint.ColorProcessor.size;

//TODO: Интегрировать в Preview.java
@Deprecated(since = "1.0.4.4")
public final class PreviewPopupService {
    public static final Preview trackNextPopup = new Preview();
    public static final Preview trackPrevPopup = new Preview();

    private static final ExecutorService executorService =
            Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "track-popup-loader");
                t.setDaemon(true);
                return t;
            });

    public static void updateTrackPopup(Preview preview, double x, double y, Track tr) {
        executorService.submit(() -> {
            try {
                javafx.scene.image.Image newImg = tr.getIndependentAlbumArt(size, size, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth);
                Color clr = ColorProcessor.core.getGeneralColorFromImage(newImg);
                String title1 = tr.getTitle();
                String artist1 = tr.getArtist();

                Platform.runLater(() -> preview.Build(x, y, newImg, artist1, title1, clr));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void updateAll() {
        updateNextTrackPopup();
        updatePrevTrackPopup();
    }

    public static void updateNextTrackPopup() {
        int pointer = PlayProcessor.playProcessor.getTrackIter() + 1;

        if(pointer >= PlayProcessor.playProcessor.getTracks().size() - 1)
            pointer = 0;

        updateTrackPopup(trackNextPopup, Root.rootImpl.btnNext.getLayoutX() - 40, Root.rootImpl.btnNext.getLayoutY() - 150, PlayProcessor.playProcessor.getTracks().get(pointer));
    }

    public static void updatePrevTrackPopup() {
        int pointer = PlayProcessor.playProcessor.getTrackIter() - 1;

        if(pointer <= 0)
            pointer = PlayProcessor.playProcessor.getTracks().size() - 1;

        updateTrackPopup(trackPrevPopup, Root.rootImpl.btnDown.getLayoutX() - 40, Root.rootImpl.btnDown.getLayoutY() - 150, PlayProcessor.playProcessor.getTracks().get(pointer));
    }

    public static void initializePaneTrackPopup() {
        Root.rootImpl.btnNext.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            Platform.runLater(() -> {
                trackNextPopup.getPane().setLayoutX(Root.rootImpl.btnNext.getLayoutX() - 40);
                trackNextPopup.getPane().setLayoutY(Root.rootImpl.btnNext.getLayoutY() - 150);

                trackNextPopup.animationEnter(Root.rootImpl.root).play();
            });
        });

        Root.rootImpl.btnNext.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            Platform.runLater(() -> {
                trackNextPopup.animationExit(Root.rootImpl.root).play();
            });
        });

        Root.rootImpl.btnNext.setOnAction((e) -> {
            Platform.runLater(PreviewPopupService::updateNextTrackPopup);
        });

        Root.rootImpl.btnDown.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            Platform.runLater(() -> {
                trackPrevPopup.getPane().setLayoutX(Root.rootImpl.btnDown.getLayoutX() - 40);
                trackPrevPopup.getPane().setLayoutY(Root.rootImpl.btnDown.getLayoutY() - 150);

                trackPrevPopup.animationEnter(Root.rootImpl.root).play();
            });
        });

        Root.rootImpl.btnDown.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            Platform.runLater(() -> {
                trackPrevPopup.animationExit(Root.rootImpl.root).play();
            });
        });

        Root.rootImpl.btnDown.setOnAction((e) -> {
            Platform.runLater(PreviewPopupService::updatePrevTrackPopup);
        });
    }
}