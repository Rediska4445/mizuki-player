package ebanina.media;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.ebanina.Player.TrackHistory;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TrackHistoryTest extends ApplicationTest {

    private ContextMenu contextMenu;
    private TrackHistory history;

    @BeforeEach
    public void setUp() {
        contextMenu = new ContextMenu();
        history = new TrackHistory(3, contextMenu);
    }

    @Test
    public void testAddAndContains() throws Exception {
        Track track1 = new Track("file:///music/song1.mp3");
        Track track2 = new Track("file:///music/song2.mp3");

        assertFalse(history.contains(track1));

        history.add(track1);
        assertTrue(history.contains(track1));
        assertEquals(1, history.size());

        history.add(track1); // дубликат не добавится
        assertEquals(1, history.size());

        history.add(track2);
        assertEquals(2, history.size());
    }

    @Test
    public void testAddExceedMaxSizeRemovesOldest() throws Exception {
        history.setMaxSize(3);

        history.add(new Track("file:///music/1.mp3"));
        history.add(new Track("file:///music/2.mp3"));
        history.add(new Track("file:///music/3.mp3"));

        for (Track track : history.getHistory()) {
            ebanina.Test.logService.println(track);
        }

        assertEquals(3, history.size());

        history.add(new Track("file:///music/4.mp3")); // превышает maxSize=3

        assertEquals(3, history.size());

        assertFalse(history.contains(new Track("file:///music/1.mp3")));
    }

    @Test
    public void testBackAndForwardNavigation() throws Exception {
        Track track1 = new Track("file:///music/one.mp3");
        Track track2 = new Track("file:///music/two.mp3");
        Track track3 = new Track("file:///music/three.mp3");
        Track track4 = new Track("file:///music/four.mp3");

        history.add(track1);
        history.add(track2);
        history.add(track3);

        // Тестируем безопасную навигацию (без исключений)
        assertEquals(track2, history.back());  // 2→1
        assertEquals(track1, history.back());  // 1→0
        assertEquals(track1, history.back());  // 0→0 (остановка)

        assertEquals(track2, history.forward()); // 0→1
        assertEquals(track3, history.forward()); // 1→2
        assertEquals(track3, history.forward()); // 2→2 (остановка)

        // Проверяем сброс итератора после add()
        history.add(new Track("file:///music/four.mp3"));
        assertEquals(track4, history.forward()); // новый итератор: 0→1
    }

    @Test
    public void testRemoveTrackFromHistoryAndMenu() throws Exception {
        Track track = new Track("file:///music/remove.mp3");
        history.add(track);
        assertTrue(history.contains(track));
        assertEquals(1, contextMenu.getItems().size());

        history.remove(track);

        assertFalse(history.contains(track));
        // Асинхронное обновление меню. Ждем в JavaFX потоке
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            assertFalse(contextMenu.getItems().stream().anyMatch(item -> item.getText().contains("remove.mp3")));
            latch.countDown();
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testSaveAndLoadFromFile() throws Exception {
        Track track1 = new Track("file:///music/save1.mp3");
        Track track2 = new Track("file:///music/save2.mp3");
        history.add(track1);
        history.add(track2);

        File tempFile = File.createTempFile("testHistory", ".dat");
        tempFile.deleteOnExit();

        history.saveToFile(tempFile);

        TrackHistory loadedHistory = new TrackHistory(10, new ContextMenu());
        loadedHistory.loadFromFile(tempFile);

        // Ждем некоторое время обновления ContextMenu (асинхронно)
        Thread.sleep(500);

        assertEquals(2, loadedHistory.size());
        assertTrue(loadedHistory.contains(track1));
        assertTrue(loadedHistory.contains(track2));

        tempFile.delete();
    }

    @Test
    public void testCreateMenuItem() {
        Track track = new Track("file:///music/song.mp3");
        track.setLastTimeTrack("34");
        var item = history.createMenuItem(track);

        assertNotNull(item);
        assertTrue(item.getText().contains(track.viewName()));
    }
}
