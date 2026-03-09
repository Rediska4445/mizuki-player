package rf.ebanina.UI.UI.Element.Buttons.Playlist;

import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.Buttons.Button;

public class NextPlaylistButton extends Button {
    public NextPlaylistButton() {
        super(ResourceManager.Instance.loadSVG("nextPlaylistButton"));
    }
}
