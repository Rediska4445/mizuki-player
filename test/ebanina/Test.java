package ebanina;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import rf.ebanina.utils.loggining.Log;

import java.io.File;
import java.nio.file.Path;

public class Test extends Application {
    public static Log logService = new Log();

    private static volatile boolean javafxInitialized = false;

    static {
        Platform.startup(() -> {});
    }

    public static synchronized void initJavaFX() throws InterruptedException {
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

    @Override
    public void start(Stage stage) throws Exception {
        initJavaFX();
    }
}
