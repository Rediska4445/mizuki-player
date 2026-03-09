package rf.ebanina.UI.UI.Popup;

import javafx.application.Platform;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static rf.ebanina.UI.Root.*;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.size;

// FIXME: Текст прикреплён к левой стороне

//TODO: Интегрировать в Preview.java
@Deprecated(since = "1.0.4.4")
public final class PreviewPopupService {
    public static final Preview trackNextPopup = new Preview();
    public static final Preview trackPrevPopup = new Preview();

    private static final ExecutorService exec = Executors.newFixedThreadPool(2);

    public static void updateTrackPopup(Preview preview, double x, double y, Track tr) {
        exec.submit(() -> {
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

        updateTrackPopup(trackNextPopup, btnNext.getLayoutX() - 40, btnNext.getLayoutY() - 150, PlayProcessor.playProcessor.getTracks().get(pointer));
    }

    public static void updatePrevTrackPopup() {
        int pointer = PlayProcessor.playProcessor.getTrackIter() - 1;

        if(pointer <= 0)
            pointer = PlayProcessor.playProcessor.getTracks().size() - 1;

        updateTrackPopup(trackPrevPopup, btnDown.getLayoutX() - 40, btnDown.getLayoutY() - 150, PlayProcessor.playProcessor.getTracks().get(pointer));
    }

    public static void initializePaneTrackPopup() {
        btnNext.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            Platform.runLater(() -> {
                trackNextPopup.getPane().setLayoutX(btnNext.getLayoutX() - 40);
                trackNextPopup.getPane().setLayoutY(btnNext.getLayoutY() - 150);

                trackNextPopup.animationEnter(root).play();
            });
        });

        btnNext.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            Platform.runLater(() -> {
                trackNextPopup.animationExit(root).play();
            });
        });

        btnNext.setOnAction((e) -> {
            Platform.runLater(PreviewPopupService::updatePrevTrackPopup);
        });

        btnDown.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            Platform.runLater(() -> {
                trackPrevPopup.getPane().setLayoutX(btnDown.getLayoutX() - 40);
                trackPrevPopup.getPane().setLayoutY(btnDown.getLayoutY() - 150);

                trackPrevPopup.animationEnter(root).play();
            });
        });

        btnDown.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            Platform.runLater(() -> {
                trackPrevPopup.animationExit(root).play();
            });
        });

        btnDown.setOnAction((e) -> {
            Platform.runLater(PreviewPopupService::updatePrevTrackPopup);
        });

        // TODO: Сделать превью для кнопок плейлиста
    }
}
