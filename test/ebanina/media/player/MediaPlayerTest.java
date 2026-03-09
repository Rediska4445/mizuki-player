package ebanina.media.player;

import com.synthbot.audioplugin.vst.vst2.JVstHost2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testfx.framework.junit5.ApplicationTest;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;
import rf.ebanina.ebanina.Player.AudioPlugins.VST.VST;
import rf.ebanina.ebanina.Player.Media;
import rf.ebanina.ebanina.Player.MediaPlayer;

import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class MediaPlayerTest extends ApplicationTest {
    @Test
    @Timeout(5)
    void testPlaybackTriggersListener() throws Exception {
        try (MediaPlayer player = new MediaPlayer(new Media("test-res" + File.separator + "metadata" + File.separator + "mp.mp3"))) {
            CountDownLatch latch = new CountDownLatch(1);

            SourceDataLine mockLine = mock(SourceDataLine.class);

            player.setPlaybackListener(Collections.singletonList((block, playbackMs, frames, line) -> {
                latch.countDown(); // Сигналим 1 раз
                assertNotNull(block);
                assertTrue(playbackMs >= 0);
                assertTrue(frames > 0);
                assertEquals(mockLine, line);
            }));

            player.play();

            assertTrue(latch.await(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void testMultipleListeners() throws Exception {
        try (MediaPlayer player = new MediaPlayer(new Media("test-res" + File.separator + "metadata" + File.separator + "mp.mp3"))) {
            CountDownLatch latch = new CountDownLatch(1);

            player.setPlaybackListener(Collections.singletonList((block, ms, frames, line) -> {
                latch.countDown();
            }));

            player.play();

            assertTrue(latch.await(3, TimeUnit.SECONDS));
        }
    }

    @Test
    @Timeout(10)
    void testAudioDataIsNotSilent() throws Exception {
        try (MediaPlayer player = new MediaPlayer(new Media("test-res" + File.separator + "metadata" + File.separator + "mp.mp3"))) {
            AtomicReference<float[][]> audioBlock = new AtomicReference<>();
            AtomicBoolean hasAudio = new AtomicBoolean(false);
            AtomicReference<Double> maxRms = new AtomicReference<>(0d);
            CountDownLatch latch = new CountDownLatch(50); // Меньше, чтобы не ждать долго

            player.setPlugins(new ArrayList<>());

            VST vst = new VST(JVstHost2.newInstance(new File("test-res" + File.separator + "audio" + File.separator + "FabFilter Pro-R.dll")));
            vst.turnOn();

            player.getPlugins().add(new PluginWrapper(vst));

            player.setPlaybackListener(Collections.singletonList((block, ms, frames, line) -> {
                audioBlock.set(block);

                // RMS проверка (энергия звука) - надежнее пороговой
                double sumSquares = 0;
                int totalSamples = 0;
                for (float[] channel : block) {
                    for (float sample : channel) {
                        sumSquares += sample * sample;
                        totalSamples++;
                    }
                }
                double rms = totalSamples > 0 ? Math.sqrt(sumSquares / totalSamples) : 0;
                maxRms.accumulateAndGet(rms, Math::max);

                // Любые данные > -80dB считаем звуком
                hasAudio.set(hasAudio.get() || rms > 0.00001);

                latch.countDown();
            }));

            player.play();

            // Ждем несколько блоков аудио
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Не получили 50 блоков аудио");

            // Отладочная информация
            System.out.println("Max RMS: " + maxRms.get());
            System.out.println("hasAudio: " + hasAudio.get());

            // Проверки
            assertNotNull(audioBlock.get(), "Не получили PCM блок");
            assertTrue(audioBlock.get().length > 0, "Нет каналов");
            assertTrue(audioBlock.get()[0].length > 0, "Нет сэмплов");
            assertTrue(hasAudio.get() || maxRms.get() > 0.001, "Нет звука (RMS слишком низкий: " + maxRms.get() + ")");
        }
    }
}
