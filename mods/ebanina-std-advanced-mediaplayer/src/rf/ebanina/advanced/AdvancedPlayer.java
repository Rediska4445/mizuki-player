package rf.ebanina.advanced;

import rf.ebanina.File.Metadata.Formats.IFormatAudioMetadata;
import rf.ebanina.File.Metadata.Formats.MP3;
import rf.ebanina.File.Metadata.Formats.WAV;
import rf.ebanina.ebanina.Player.MediaPlayer;

public class AdvancedPlayer extends MediaPlayer {
    public void render(IFormatAudioMetadata formatAudioMetadata) {
        if(formatAudioMetadata instanceof MP3) {

        } else if(formatAudioMetadata instanceof WAV) {

        }
    }
}