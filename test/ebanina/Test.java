package ebanina;

import javafx.application.Platform;
import rf.ebanina.utils.loggining.Log;

import java.io.File;
import java.nio.file.Path;

public class Test
{
    public static Log logService = new Log();

    private static volatile boolean javafxInitialized = false;

    public static synchronized void initJavaFX()
            throws InterruptedException
    {
        if (!javafxInitialized) {
            if (!Platform.isFxApplicationThread()) {
                Platform.startup(() -> {});
            }
            Thread.sleep(500);
            javafxInitialized = true;
        }
    }

    protected Path guineaPigs(String relative) {
        return Path.of("test-res" + File.separator + relative);
    }

    protected Path guineaMp3Pigs() {
        return Path.of("test-res" + File.separator + "metadata" + File.separator + "mp.mp3");
    }

    protected Path guineaWavPigs(String relative) {
        return Path.of("test-res" + File.separator + "metadata" + File.separator + "wav.mp3");
    }

    protected Path guineaVstPlugin() {
        return Path.of("test-res" + File.separator + "audio" + File.separator + "FabFilter Pro-R.dll");
    }
}
