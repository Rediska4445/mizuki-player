package rf.ebanina.ebanina.Player;

import rf.ebanina.File.Resources.ResourceManager;

import java.io.File;

public class AudioVolumer {
    public AudioVolumer() {
        System.load(ResourceManager.BIN_LIBRARIES_PATH + File.separator + "VolumeLib.dll");
    }

    public native float getSystemVolume();

    public native void setSystemVolume(float volume);

    public static AudioVolumer instance = new AudioVolumer();
}
