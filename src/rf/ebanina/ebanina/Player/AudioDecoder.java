package rf.ebanina.ebanina.Player;

import javax.sound.sampled.AudioInputStream;
import java.io.File;

public interface AudioDecoder {
    String getFormat();

    double computeAudioDuration(File var1);

    AudioInputStream createStreaming(File var1);
}